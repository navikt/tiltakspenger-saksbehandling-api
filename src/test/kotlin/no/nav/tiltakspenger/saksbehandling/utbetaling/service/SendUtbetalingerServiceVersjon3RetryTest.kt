package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.setup.MdcKorrelasjon
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.hentUtbetalingsstatistikkIder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ProdusertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingProducer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.tilHelvedUtbetalingJson
import org.junit.jupiter.api.Test

class SendUtbetalingerServiceVersjon3RetryTest {

    @Test
    @IsolatedDatabaseTest
    fun `requestsnapshot er first-write-wins og bevarer andre metadatafelt`() {
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            oppdragsversjonForNyeSaker = Oppdragsversjon.V3,
        ) { tac ->
            val (_, utbetaling) = opprettUtbetaling(tac)
            val repo = tac.utbetalingContext.utbetalingRepo
            tac.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                        UPDATE utbetaling
                        SET utbetaling_metadata = '{"tillegg":"behold"}'::jsonb
                        WHERE id = :id
                        """,
                        "id" to utbetaling.id.toString(),
                    ).asUpdate,
                )
            }

            val request = """ { "raw" : "linje\n" } """
            val lagretFørsteGang = tac.sessionFactory.withTransactionContext { tx ->
                repo.lagreUtbetalingJsonHvisMangler(utbetaling.id, request, tx)
            }
            val lagretAndreGang = tac.sessionFactory.withTransactionContext { tx ->
                repo.lagreUtbetalingJsonHvisMangler(utbetaling.id, """{"annen":"body"}""", tx)
            }
            tac.sessionFactory.withTransactionContext { tx ->
                repo.lagreUtbetalingsrespons(utbetaling.id, ProdusertUtbetaling(request), tx)
            }
            val metadata = tac.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                        SELECT utbetaling_metadata->>'request' AS request,
                               utbetaling_metadata->>'tillegg' AS tillegg
                        FROM utbetaling
                        WHERE id = :id
                        """,
                        "id" to utbetaling.id.toString(),
                    ).map { row -> row.stringOrNull("request") to row.stringOrNull("tillegg") }.asSingle,
                )
            }
            val manglendeUtbetaling = tac.sessionFactory.withTransactionContext { tx ->
                repo.lagreUtbetalingJsonHvisMangler(UtbetalingId.random(), request, tx)
            }

            (lagretFørsteGang == request) shouldBe true
            (lagretAndreGang == request) shouldBe true
            (metadata?.first == request) shouldBe true
            metadata?.second shouldBe "behold"
            manglendeUtbetaling shouldBe null
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `lagring av produsert utbetaling avviser en annen request enn snapshot`() {
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            oppdragsversjonForNyeSaker = Oppdragsversjon.V3,
        ) { tac ->
            val (_, utbetaling) = opprettUtbetaling(tac)
            val repo = tac.utbetalingContext.utbetalingRepo
            val lagretRequest = """ { "raw" : "første" } """
            val annenRequest = """{"raw":"andre"}"""

            tac.sessionFactory.withTransactionContext { tx ->
                repo.lagreUtbetalingJsonHvisMangler(utbetaling.id, lagretRequest, tx)
            } shouldBe lagretRequest

            shouldThrow<IllegalStateException> {
                tac.sessionFactory.withTransactionContext { tx ->
                    repo.lagreUtbetalingsrespons(utbetaling.id, ProdusertUtbetaling(annenRequest), tx)
                }
            }

            repo.hentUtbetalingJson(utbetaling.id) shouldBe lagretRequest
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `snapshot commitfeil stopper før Kafka`() {
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            oppdragsversjonForNyeSaker = Oppdragsversjon.V3,
        ) { tac ->
            val (sak, utbetaling) = opprettUtbetaling(tac)
            val repo = tac.utbetalingContext.utbetalingRepo
            val producer = RecordingUtbetalingProducer()
            val service = sendService(tac, producer)
            tac.opprettSnapshotCommitfeil()

            service.sendUtbetalingerTilHelved()

            tac.fjernSnapshotCommitfeil()
            (producer.meldinger.isEmpty()) shouldBe true
            (repo.hentUtbetalingJson(utbetaling.id) == null) shouldBe true
            hentUtbetaling(tac, utbetaling.id).sendtTilUtbetaling shouldBe null
            tac.sessionFactory.hentUtbetalingsstatistikkIder(sak.id) shouldBe emptyList()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `Kafka-feil ruller tilbake sendtmarkering og statistikk men beholder snapshot`() {
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            oppdragsversjonForNyeSaker = Oppdragsversjon.V3,
        ) { tac ->
            val (sak, utbetaling) = opprettUtbetaling(tac)
            var antallForsøk = 0
            val producer = object : UtbetalingProducer {
                override fun produser(utbetalingId: UtbetalingId, helvedUtbetalingJson: String) {
                    antallForsøk += 1
                    error("Kafka feilet")
                }
            }
            val service = sendService(tac, producer)

            service.sendUtbetalingerTilHelved()

            antallForsøk shouldBe 1
            (tac.utbetalingContext.utbetalingRepo.hentUtbetalingJson(utbetaling.id) != null) shouldBe true
            hentUtbetaling(tac, utbetaling.id).sendtTilUtbetaling shouldBe null
            tac.sessionFactory.hentUtbetalingsstatistikkIder(sak.id) shouldBe emptyList()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `retry etter sendetransaksjonsfeil bruker samme nøkkel og byte-identiske request etter identbytte`() {
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            oppdragsversjonForNyeSaker = Oppdragsversjon.V3,
        ) { tac ->
            val (sak, utbetaling) = opprettUtbetaling(tac)
            val repo = tac.utbetalingContext.utbetalingRepo
            val producer = RecordingUtbetalingProducer()
            val service = sendService(tac, producer)
            tac.opprettSendetransaksjonsfeil()

            service.sendUtbetalingerTilHelved()

            tac.fjernSendetransaksjonsfeil()
            val førsteRequest = repo.hentUtbetalingJson(utbetaling.id)
            (førsteRequest != null) shouldBe true
            hentUtbetaling(tac, utbetaling.id).sendtTilUtbetaling shouldBe null
            tac.sessionFactory.hentUtbetalingsstatistikkIder(sak.id) shouldBe emptyList()

            val nyttFnr = Fnr.random()
            (utbetaling.fnr != nyttFnr) shouldBe true
            tac.sessionFactory.withTransactionContext { tx ->
                tac.sakContext.sakRepo.oppdaterFnr(utbetaling.fnr, nyttFnr, tx)
            }
            val oppdatertUtbetaling = hentUtbetaling(tac, utbetaling.id)
            (oppdatertUtbetaling.fnr == nyttFnr) shouldBe true
            val nyKandidat = oppdatertUtbetaling.tilHelvedUtbetalingJson(null)
            (førsteRequest!!.contains(utbetaling.fnr.verdi)) shouldBe true
            (nyKandidat.contains(nyttFnr.verdi)) shouldBe true
            (førsteRequest != nyKandidat) shouldBe true

            sendService(tac, producer).sendUtbetalingerTilHelved()

            producer.meldinger.size shouldBe 2
            producer.meldinger.map { it.first } shouldBe listOf(utbetaling.id, utbetaling.id)
            (producer.meldinger[0].second == producer.meldinger[1].second) shouldBe true
            (førsteRequest == producer.meldinger[0].second) shouldBe true
            (producer.meldinger[0].second == repo.hentUtbetalingJson(utbetaling.id)) shouldBe true
            (hentUtbetaling(tac, utbetaling.id).sendtTilUtbetaling != null) shouldBe true
            tac.sessionFactory.hentUtbetalingsstatistikkIder(sak.id).size shouldBe 1
        }
    }

    private suspend fun ApplicationTestBuilder.opprettUtbetaling(
        tac: TestApplicationContextMedPostgres,
    ): Pair<Sak, VedtattUtbetaling> {
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            jobber = JobberEtterIverksettelse.ingen,
        )
        val (_, vedtak) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            jobber = JobberEtterIverksettelse.ingen,
        )!!
        return sak to vedtak.utbetaling
    }

    private fun hentUtbetaling(
        tac: TestApplicationContextMedPostgres,
        utbetalingId: UtbetalingId,
    ): VedtattUtbetaling =
        tac.sessionFactory.withTransactionContext { tx ->
            tac.utbetalingContext.utbetalingRepo.hent(utbetalingId, tx)!!
        }

    private fun sendService(
        tac: TestApplicationContextMedPostgres,
        producer: UtbetalingProducer,
    ) = SendUtbetalingerService(
        utbetalingRepo = tac.utbetalingContext.utbetalingRepo,
        meldekortvedtakRepo = tac.utbetalingContext.meldekortvedtakRepo,
        utbetalingsklient = tac.utbetalingContext.utbetalingsklient,
        utbetalingProducer = producer,
        statistikkService = tac.statistikkContext.statistikkService,
        sessionFactory = tac.sessionFactory,
        clock = tac.clock,
        korrelasjon = MdcKorrelasjon(),
    )

    private fun TestApplicationContextMedPostgres.opprettSnapshotCommitfeil() {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    CREATE FUNCTION fail_v3_snapshot_commit_test() RETURNS trigger
                    LANGUAGE plpgsql
                    AS ${'$'}${'$'}
                    BEGIN
                        IF OLD.utbetaling_metadata->>'request' IS NULL
                           AND NEW.utbetaling_metadata->>'request' IS NOT NULL THEN
                            RAISE EXCEPTION 'snapshot commit failure for test';
                        END IF;
                        RETURN NEW;
                    END;
                    ${'$'}${'$'}
                    """,
                ).asUpdate,
            )
            session.run(
                sqlQuery(
                    """
                    CREATE CONSTRAINT TRIGGER fail_v3_snapshot_commit_test
                    AFTER UPDATE ON utbetaling
                    DEFERRABLE INITIALLY DEFERRED
                    FOR EACH ROW
                    EXECUTE FUNCTION fail_v3_snapshot_commit_test()
                    """,
                ).asUpdate,
            )
        }
    }

    private fun TestApplicationContextMedPostgres.fjernSnapshotCommitfeil() {
        sessionFactory.withSession { session ->
            session.run(sqlQuery("DROP TRIGGER fail_v3_snapshot_commit_test ON utbetaling").asUpdate)
            session.run(sqlQuery("DROP FUNCTION fail_v3_snapshot_commit_test()").asUpdate)
        }
    }

    private fun TestApplicationContextMedPostgres.opprettSendetransaksjonsfeil() {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    CREATE FUNCTION fail_v3_send_commit_test() RETURNS trigger
                    LANGUAGE plpgsql
                    AS ${'$'}${'$'}
                    BEGIN
                        IF OLD.sendt_til_utbetaling_tidspunkt IS NULL
                           AND NEW.sendt_til_utbetaling_tidspunkt IS NOT NULL THEN
                            RAISE EXCEPTION 'send transaction commit failure for test';
                        END IF;
                        RETURN NEW;
                    END;
                    ${'$'}${'$'}
                    """,
                ).asUpdate,
            )
            session.run(
                sqlQuery(
                    """
                    CREATE CONSTRAINT TRIGGER fail_v3_send_commit_test
                    AFTER UPDATE ON utbetaling
                    DEFERRABLE INITIALLY DEFERRED
                    FOR EACH ROW
                    EXECUTE FUNCTION fail_v3_send_commit_test()
                    """,
                ).asUpdate,
            )
        }
    }

    private fun TestApplicationContextMedPostgres.fjernSendetransaksjonsfeil() {
        sessionFactory.withSession { session ->
            session.run(sqlQuery("DROP TRIGGER fail_v3_send_commit_test ON utbetaling").asUpdate)
            session.run(sqlQuery("DROP FUNCTION fail_v3_send_commit_test()").asUpdate)
        }
    }

    private class RecordingUtbetalingProducer : UtbetalingProducer {
        val meldinger = mutableListOf<Pair<UtbetalingId, String>>()

        override fun produser(utbetalingId: UtbetalingId, helvedUtbetalingJson: String) {
            meldinger.add(utbetalingId to helvedUtbetalingJson)
        }
    }
}
