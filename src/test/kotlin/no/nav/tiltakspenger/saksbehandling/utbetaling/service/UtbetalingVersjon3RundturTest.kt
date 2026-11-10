package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.Loggfanger
import no.nav.tiltakspenger.saksbehandling.common.Sikkerloggfanger
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.Utbetalingsmålinger
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeBehandleUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.HelvedStatusConsumer
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.HelvedUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.tilUtbetalingsstatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Utbetaling for en sak på [Oppdragsversjon.V3]: produsert på Kafka, med status tilbake fra Hel Ved på Kafka.
 *
 * Testen må kjøre isolert, fordi utsjekksjobben i en annen test ellers kan sende utbetalingen gjennom sin egen producer.
 */
class UtbetalingVersjon3RundturTest {

    @Test
    @IsolatedDatabaseTest
    fun `utbetalingen produseres på kafka, statusen fra Hel Ved lagres, og neste utbetaling tar med forrige meldeperiode`() {
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            oppdragsversjonForNyeSaker = Oppdragsversjon.V3,
        ) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val jobber = JobberEtterIverksettelse(oppdaterUtbetalingsstatus = false)
            val (_, førsteVedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder[0].kjedeId,
                jobber = jobber,
            )!!
            val repo = tac.utbetalingContext.meldekortvedtakRepo
            val utbetalingId = førsteVedtak.utbetaling.id

            fun meldeperioder(json: String) =
                deserialize<HelvedUtbetalingDTO>(json).perioder.map { it.meldeperiode }.distinct()

            val førsteMelding = tac.utbetalingFakeProducer.produserteUtbetalinger.getValue(utbetalingId)
            deserialize<HelvedUtbetalingDTO>(førsteMelding).sakId shouldBe sak.saksnummer.toString()
            meldeperioder(førsteMelding) shouldBe listOf(sak.meldeperiodeKjeder[0].kjedeId.toString())

            var offset = 0L

            fun konsumerStatus(json: String) {
                val record = ConsumerRecord<String, String?>(
                    "helved.status.v1",
                    0,
                    offset++,
                    utbetalingId.toString(),
                    json,
                )
                record.headers().add("fagsystem", "TILTAKSPENGER".toByteArray())
                HelvedStatusConsumer.consume(
                    record = record,
                    clock = tac.clock,
                    lagreUtbetalingskvittering = tac.utbetalingContext.utbetalingskvitteringRepo::lagreNy,
                ) shouldBe true
                // Innboksen er ikke domenetilstand på saken, så id-en leses med egen SQL, ikke jobbens køspørring.
                val id = tac.sessionFactory.withSession { session ->
                    session.run(
                        sqlQuery(
                            "SELECT id FROM utbetalingskvittering WHERE key = :key AND (kafka ->> 'offset')::bigint = :offset",
                            "key" to utbetalingId.toString(),
                            "offset" to record.offset(),
                        ).map { UtbetalingskvitteringId.fromString(it.string("id")) }.asSingle,
                    )!!
                }
                tac.utbetalingContext.behandleUtbetalingskvitteringerJobb.behandle(id).getOrFail()
                tac.sessionFactory.withTransactionContext {
                    tac.utbetalingContext.utbetalingskvitteringRepo.hent(id, it).sakId
                } shouldBe sak.id
                tac.sessionFactory.withTransactionContext {
                    tac.utbetalingContext.utbetalingskvitteringRepo.hent(id, it).utbetalingId
                } shouldBe utbetalingId
            }

            konsumerStatus("""{"status":"MOTTATT","detaljer":{"ytelse":"TILTAKSPENGER","linjer":[]}}""")
            repo.hentForVedtakId(førsteVedtak.id)!!.utbetaling.status shouldBe Utbetalingsstatus.IkkePåbegynt

            konsumerStatus("""{"status":"HOS_OPPDRAG","detaljer":{"ytelse":"TILTAKSPENGER","linjer":[]}}""")
            repo.hentForVedtakId(førsteVedtak.id)!!.utbetaling.status shouldBe Utbetalingsstatus.SendtTilOppdrag

            konsumerStatus("""{"status":"OK","detaljer":{"ytelse":"TILTAKSPENGER","linjer":[]}}""")
            repo.hentForVedtakId(førsteVedtak.id)!!.utbetaling.status shouldBe Utbetalingsstatus.Ok

            konsumerStatus("""{"status":"HOS_OPPDRAG","detaljer":{"ytelse":"TILTAKSPENGER","linjer":[]}}""")
            repo.hentForVedtakId(førsteVedtak.id)!!.utbetaling.status shouldBe Utbetalingsstatus.Ok

            val (_, andreVedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                jobber = jobber,
            )!!

            val andreMelding = tac.utbetalingFakeProducer.produserteUtbetalinger.getValue(andreVedtak.utbetaling.id)
            meldeperioder(andreMelding) shouldBe sak.meldeperiodeKjeder.take(2).map { it.kjedeId.toString() }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `ugyldig innhold knyttes til kjent sak før innholdstolkning`() {
        withTestApplicationContextAndPostgres(runIsolated = true, oppdragsversjonForNyeSaker = Oppdragsversjon.V3) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val (_, vedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder[0].kjedeId,
                jobber = JobberEtterIverksettelse(oppdaterUtbetalingsstatus = false),
            )!!
            val innboks = tac.utbetalingContext.utbetalingskvitteringRepo
            val record = ConsumerRecord<String, String?>(
                "helved.status.v1",
                0,
                0,
                vedtak.utbetaling.id.toString(),
                """{"status":"UKJENT"}""",
            )
            record.headers().add("fagsystem", "TILTAKSPENGER".toByteArray())
            HelvedStatusConsumer.consume(record, tac.clock, innboks::lagreNy) shouldBe true
            val id = tac.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery("SELECT id FROM utbetalingskvittering WHERE key = :key", "key" to record.key())
                        .map { UtbetalingskvitteringId.fromString(it.string("id")) }.asSingle,
                )!!
            }

            tac.utbetalingContext.behandleUtbetalingskvitteringerJobb.behandle(id).leftOrNull() shouldBe
                KunneIkkeBehandleUtbetalingskvittering.UgyldigInnhold
            val etterFeil = tac.sessionFactory.withTransactionContext { innboks.hent(id, it) }
            etterFeil.sakId shouldBe sak.id
            etterFeil.utbetalingId shouldBe vedtak.utbetaling.id
            etterFeil.behandlet shouldBe null
            etterFeil.forsøkshistorikk!!.antallForsøk shouldBe 1L
            innboks.lagreFeil(
                id = id,
                feil = "UgyldigInnhold",
                forbigående = false,
                forsøkshistorikk = requireNotNull(etterFeil.forsøkshistorikk),
                kobling = null,
            )
            tac.sessionFactory.withTransactionContext { innboks.hent(id, it) }.let {
                it.sakId shouldBe sak.id
                it.utbetalingId shouldBe vedtak.utbetaling.id
            }
            tac.sessionFactory.withTransactionContext { context ->
                innboks.markerBehandlet(id, nå(tac.clock), null, context)
            }
            tac.sessionFactory.withTransactionContext { innboks.hent(id, it) }.let {
                it.sakId shouldBe sak.id
                it.utbetalingId shouldBe vedtak.utbetaling.id
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `feilet ferdigmarkering ruller tilbake statusen og en ny jobb fullfører etter backoff`() {
        withTestApplicationContextAndPostgres(runIsolated = true, oppdragsversjonForNyeSaker = Oppdragsversjon.V3) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val (_, vedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder[0].kjedeId,
                jobber = JobberEtterIverksettelse(oppdaterUtbetalingsstatus = false),
            )!!
            val innboks = tac.utbetalingContext.utbetalingskvitteringRepo
            val record = ConsumerRecord<String, String?>("helved.status.v1", 0, 0, vedtak.utbetaling.id.toString(), """{"status":"OK"}""")
            record.headers().add("fagsystem", "TILTAKSPENGER".toByteArray())
            HelvedStatusConsumer.consume(record, tac.clock, innboks::lagreNy) shouldBe true
            // Egen lesekanal for innboksraden; køspørringen testes i aggregat-testen.
            val id = tac.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery("SELECT id FROM utbetalingskvittering WHERE key = :key", "key" to record.key())
                        .map { UtbetalingskvitteringId.fromString(it.string("id")) }.asSingle,
                )!!
            }
            val feilendeInnboks = object : UtbetalingskvitteringRepo by innboks {
                override fun markerBehandlet(
                    id: UtbetalingskvitteringId,
                    behandlet: LocalDateTime,
                    kobling: Utbetalingskobling?,
                    context: TransactionContext,
                ) {
                    error("Avbrudd før ferdigmarkering")
                }
            }
            val jobb = BehandleUtbetalingskvitteringerJobb(
                feilendeInnboks,
                tac.utbetalingContext.utbetalingRepo,
                ::tilUtbetalingsstatus,
                tac.clock,
                tac.sessionFactory,
                Utbetalingsmålinger(tac.meterRegistry),
                Loggfanger("kvittering"),
                Sikkerloggfanger(),
            )
            jobb.behandle(id).isLeft() shouldBe true
            tac.utbetalingContext.meldekortvedtakRepo.hentForVedtakId(vedtak.id)!!.utbetaling.status shouldBe null
            val etterFeil = tac.sessionFactory.withTransactionContext { innboks.hent(id, it) }
            etterFeil.behandlet shouldBe null
            etterFeil.forsøkshistorikk!!.antallForsøk shouldBe 1
            etterFeil.sakId shouldBe sak.id
            etterFeil.utbetalingId shouldBe vedtak.utbetaling.id

            tac.clock.spolTil(etterFeil.forsøkshistorikk.nesteForsøk)
            tac.utbetalingContext.behandleUtbetalingskvitteringerJobb.behandle(id).getOrFail()
            tac.utbetalingContext.meldekortvedtakRepo.hentForVedtakId(vedtak.id)!!.utbetaling.status shouldBe Utbetalingsstatus.OkUtenUtbetaling
            val behandlet = tac.sessionFactory.withTransactionContext { innboks.hent(id, it) }
            behandlet.sakId shouldBe sak.id
            behandlet.utbetalingId shouldBe vedtak.utbetaling.id
            behandlet.behandlet!!
                .isBefore(etterFeil.forsøkshistorikk.nesteForsøk) shouldBe false
        }
    }
}
