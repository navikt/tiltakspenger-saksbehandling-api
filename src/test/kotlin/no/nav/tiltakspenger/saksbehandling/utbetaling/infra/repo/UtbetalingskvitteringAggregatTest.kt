package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.RåUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringId
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.HelvedStatusConsumer
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.kafkametadata
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Aggregat-test for innboksen med utbetalingskvitteringer, jf. testtaksonomien i `AGENTS.md`. */
class UtbetalingskvitteringAggregatTest {
    @Test
    @IsolatedDatabaseTest
    fun `tomt referansesett gir ingen utbetalingstreff`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val treff = tac.sessionFactory.withTransactionContext { context ->
                tac.utbetalingContext.utbetalingRepo.finnUtbetalingerForKvitteringsreferanser(emptySet(), context)
            }

            treff shouldBe emptyList()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `koblingsfelt forblir tomme eller beholdes når Postgresrepo får null eller ikke-null kobling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val kvitteringsrepo = tac.utbetalingContext.utbetalingskvitteringRepo
            val tidspunkt = nå(tac.clock)
            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val (_, vedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                jobber = JobberEtterIverksettelse.ingen,
            )!!
            val kobling = Utbetalingskobling(vedtak.utbetaling.id, sak.id)

            fun lagreKvittering(offset: Long): UtbetalingskvitteringId {
                val key = "koblingstest-$offset"
                kvitteringsrepo.lagreNy(
                    key = key,
                    value = "rå innhold",
                    kafkametadata = kafkametadata(offset = offset, partisjon = 0),
                    mottatt = tidspunkt,
                ) shouldBe true
                return tac.sessionFactory.withSession { session ->
                    session.run(
                        sqlQuery(
                            "SELECT id FROM utbetalingskvittering WHERE key = :key",
                            "key" to key,
                        ).map { UtbetalingskvitteringId.fromString(it.string("id")) }.asSingle,
                    )!!
                }
            }

            fun les(id: UtbetalingskvitteringId) =
                tac.sessionFactory.withTransactionContext { context -> kvitteringsrepo.hent(id, context) }

            val utenKoblingVedFerdigmarkering = lagreKvittering(100)
            tac.sessionFactory.withTransactionContext { context ->
                kvitteringsrepo.markerBehandlet(utenKoblingVedFerdigmarkering, tidspunkt, null, context)
            }
            les(utenKoblingVedFerdigmarkering).let {
                it.sakId shouldBe null
                it.utbetalingId shouldBe null
            }

            val medKoblingVedFerdigmarkering = lagreKvittering(101)
            tac.sessionFactory.withTransactionContext { context ->
                kvitteringsrepo.markerBehandlet(medKoblingVedFerdigmarkering, tidspunkt, kobling, context)
            }
            tac.sessionFactory.withTransactionContext { context ->
                kvitteringsrepo.markerBehandlet(medKoblingVedFerdigmarkering, tidspunkt, null, context)
            }
            les(medKoblingVedFerdigmarkering).let {
                it.sakId shouldBe sak.id
                it.utbetalingId shouldBe vedtak.utbetaling.id
            }

            val historikk = Forsøkshistorikk(
                forrigeForsøk = tidspunkt.minusMinutes(1),
                nesteForsøk = tidspunkt,
                antallForsøk = 1,
            )
            val utenKoblingVedFeil = lagreKvittering(102)
            kvitteringsrepo.lagreFeil(utenKoblingVedFeil, "testfeil", true, historikk, null)
            les(utenKoblingVedFeil).let {
                it.sakId shouldBe null
                it.utbetalingId shouldBe null
            }

            val medKoblingVedFeil = lagreKvittering(103)
            kvitteringsrepo.lagreFeil(medKoblingVedFeil, "testfeil", true, historikk, kobling)
            kvitteringsrepo.lagreFeil(medKoblingVedFeil, "testfeil", true, historikk, null)
            les(medKoblingVedFeil).let {
                it.sakId shouldBe sak.id
                it.utbetalingId shouldBe vedtak.utbetaling.id
            }

            val kunSak = lagreKvittering(104)
            tac.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        "UPDATE utbetalingskvittering SET sak_id = :sak_id WHERE id = :id",
                        "sak_id" to sak.id.toString(),
                        "id" to kunSak.toString(),
                    ).asUpdate,
                )
            } shouldBe 1
            kvitteringsrepo.lagreFeil(kunSak, "testfeil", true, historikk, null)
            tac.sessionFactory.withTransactionContext { context ->
                kvitteringsrepo.markerBehandlet(kunSak, tidspunkt, null, context)
            }
            les(kunSak).let {
                it.sakId shouldBe sak.id
                it.utbetalingId shouldBe null
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `V2-kvittering kobles via linjereferanse og rydder tidligere feil uten statusendring`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val repo = tac.utbetalingContext.utbetalingskvitteringRepo
            val nå = nå(tac.clock)
            val (sak) = iverksettSøknadsbehandling(tac = tac, jobber = JobberEtterIverksettelse.ingen)
            val (_, vedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                jobber = JobberEtterIverksettelse.ingen,
            )!!
            val utbetaling = vedtak.utbetaling
            val nøkkel = "550e8400-e29b-41d4-a716-446655440000"
            val råinnhold = """{"status":"OK","detaljer":{"ytelse":"TILTAKSPENGER","linjer":[{"behandlingId":"${utbetaling.id.uuidPart()}"}]}}"""
            repo.lagreNy(
                key = nøkkel,
                value = råinnhold,
                kafkametadata = kafkametadata(offset = 1, partisjon = 0),
                mottatt = nå,
            ) shouldBe true
            val id = repo.hentUbehandledeIder(nå, limit = 1).single()
            repo.lagreFeil(
                id = id,
                feil = "UgyldigInnhold",
                forbigående = false,
                forsøkshistorikk = Forsøkshistorikk(
                    forrigeForsøk = nå.minusMinutes(1),
                    nesteForsøk = nå,
                    antallForsøk = 1,
                ),
                kobling = null,
            )
            repo.antallPermanenteFeil() shouldBe 1L

            tac.utbetalingContext.behandleUtbetalingskvitteringerJobb.behandle(id).getOrFail()

            val behandlet = tac.sessionFactory.withTransactionContext { context -> repo.hent(id, context) }
            requireNotNull(behandlet.behandlet)
            behandlet.key shouldBe nøkkel
            behandlet.value shouldBe råinnhold
            behandlet.sakId shouldBe sak.id
            behandlet.utbetalingId shouldBe utbetaling.id
            tac.sessionFactory.withTransactionContext { context ->
                tac.utbetalingContext.utbetalingRepo.hent(utbetaling.id, context)?.status
            } shouldBe null
            repo.antallPermanenteFeil() shouldBe 0L
            val feiltilstand = tac.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                        select behandlet_feil, forbigaaende::text as forbigaaende
                        from utbetalingskvittering
                        where id = :id
                        """,
                        "id" to id.toString(),
                    ).map {
                        it.stringOrNull("behandlet_feil") to it.stringOrNull("forbigaaende")
                    }.asSingle,
                )
            }
            feiltilstand shouldBe (null to null)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `konsumerinngangen lagrer rå melding én gang per topic partisjon og offset`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val repo = tac.utbetalingContext.utbetalingskvitteringRepo

            fun konsumer(
                topic: String,
                partisjon: Int,
                offset: Long,
                key: String,
                value: String?,
            ): Boolean {
                val record = ConsumerRecord<String, String?>(topic, partisjon, offset, key, value)
                record.headers().add("fagsystem", "TILTAKSPENGER".toByteArray())
                return HelvedStatusConsumer.consume(record, tac.clock) { recordKey, recordValue, metadata, mottatt ->
                    repo.lagreNy(recordKey, recordValue, metadata, mottatt)
                }
            }

            konsumer("helved.status.v1", 0, 1, "utbetaling-1", "ikke JSON {") shouldBe true
            konsumer("helved.status.v1", 0, 1, "duplikat", """{"status":"OK"}""") shouldBe false
            konsumer("helved.status.v1", 1, 1, "utbetaling-2", """{ "status" : "OK" }""") shouldBe true
            konsumer("annet.topic", 0, 1, "utbetaling-3", null) shouldBe true

            val mottatt = nå(tac.clock)
            repo.lagreNy(
                key = null,
                value = null,
                kafkametadata = kafkametadata(offset = 2, partisjon = 2),
                mottatt = mottatt,
            ) shouldBe true
            repo.lagreNy(
                key = null,
                value = "rå payload",
                kafkametadata = kafkametadata(offset = 3, partisjon = 2),
                mottatt = mottatt,
            ) shouldBe true

            val ids = repo.hentUbehandledeIder(nå(tac.clock), limit = 10)
            ids.size shouldBe 5
            repo.hentUbehandledeIder(nå(tac.clock), limit = 2) shouldBe ids.take(2)

            val kvitteringer = tac.sessionFactory.withTransactionContext { context ->
                ids.map { repo.hent(it, context) }
            }
            kvitteringer.all { it.id.toString().startsWith("utbetalingskvittering_") } shouldBe true
            kvitteringer.map { it.key } shouldBe listOf("utbetaling-3", "utbetaling-1", "utbetaling-2", null, null)
            kvitteringer.map { it.value } shouldBe
                listOf(null, "ikke JSON {", """{ "status" : "OK" }""", null, "rå payload")
            kvitteringer.map { it.behandlet } shouldBe listOf(null, null, null, null, null)
            kvitteringer.map { it.forsøkshistorikk } shouldBe listOf(null, null, null, null, null)
            kvitteringer.map { it.sakId } shouldBe listOf(null, null, null, null, null)
            kvitteringer.map { it.utbetalingId } shouldBe listOf(null, null, null, null, null)

            shouldThrow<IllegalStateException> {
                tac.sessionFactory.withTransactionContext { context -> repo.hent(UtbetalingskvitteringId.random(), context) }
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `Kafka-offset styrer rekkefølge uavhengig av ULID og innsettingsrekkefølge`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val repo = tac.utbetalingContext.utbetalingskvitteringRepo
            val tidspunktVedTeststart = nå(tac.clock)
            val mottattPerTestverdi = mutableMapOf<String, LocalDateTime>()

            fun konsumer(topic: String, partisjon: Int, offset: Long, key: String?, label: String) {
                val value = """{"simulering":true,"testverdi":"$label"}"""
                val record = ConsumerRecord<String, String?>(topic, partisjon, offset, key, value)
                record.headers().add("fagsystem", "TILTAKSPENGER".toByteArray())
                HelvedStatusConsumer.consume(record, tac.clock) { recordKey, recordValue, metadata, tidspunkt ->
                    mottattPerTestverdi[label] = tidspunkt
                    repo.lagreNy(recordKey, recordValue, metadata, tidspunkt)
                } shouldBe true
            }

            konsumer("helved.status.v1", 0, 10, "offset-10", "annen-nøkkel-offset-10")
            konsumer("helved.status.v1", 0, 2, "offset-2", "annen-nøkkel-offset-2")
            konsumer("helved.status.v1", 0, 1, "samme", "samme-nøkkel-partisjon-0")
            konsumer("helved.status.v1", 0, 5, null, "null-nøkkel-offset-5")
            konsumer("helved.status.v1", 0, 6, null, "null-nøkkel-offset-6")
            konsumer("helved.status.v1", 1, 10, "samme", "høyere-offset")
            konsumer("helved.status.v1", 1, 2, "samme", "lavere-offset")
            konsumer("annet.topic", 0, 1, "samme", "samme-nøkkel-annet-topic")

            fun id(suffiks: Int) =
                UtbetalingskvitteringId.fromString(
                    "utbetalingskvittering_01HSTRQBRM443VGB4WA822TE${suffiks.toString().padStart(2, '0')}",
                )

            // ID-ene er tilfeldig generert i prodstien; testverdiene settes motsatt av offset for å sikre at ID-rekkefølgen ikke styrer køen.
            fun settId(topic: String, partisjon: Int, offset: Long, id: UtbetalingskvitteringId) {
                val oppdatert = tac.sessionFactory.withSession { session ->
                    session.run(
                        sqlQuery(
                            """
                            update utbetalingskvittering
                            set id = :id
                            where kafka ->> 'topic' = :topic
                              and (kafka ->> 'partisjon')::int = :partisjon
                              and (kafka ->> 'offset')::bigint = :offset
                            """,
                            "id" to id.toString(),
                            "topic" to topic,
                            "partisjon" to partisjon,
                            "offset" to offset,
                        ).asUpdate,
                    )
                }
                oppdatert shouldBe 1
            }

            val annenNøkkelOffset10Id = id(1)
            val høyereOffsetId = id(2)
            val sammeNøkkelPartisjon0Id = id(3)
            val nullNøkkelOffset6Id = id(4)
            val nullNøkkelOffset5Id = id(5)
            val annenNøkkelOffset2Id = id(6)
            val lavereOffsetId = id(7)
            val annetTopicId = id(8)
            settId("helved.status.v1", 0, 10, annenNøkkelOffset10Id)
            settId("helved.status.v1", 1, 10, høyereOffsetId)
            settId("helved.status.v1", 0, 1, sammeNøkkelPartisjon0Id)
            settId("helved.status.v1", 0, 6, nullNøkkelOffset6Id)
            settId("helved.status.v1", 0, 5, nullNøkkelOffset5Id)
            settId("helved.status.v1", 0, 2, annenNøkkelOffset2Id)
            settId("helved.status.v1", 1, 2, lavereOffsetId)
            settId("annet.topic", 0, 1, annetTopicId)

            fun verdier(ids: List<UtbetalingskvitteringId>): List<String?> =
                tac.sessionFactory.withTransactionContext { context ->
                    ids.map { lesTre(requireNotNull(repo.hent(it, context).value)).path("testverdi").asString() }
                }

            fun verdierPå(tidspunkt: LocalDateTime) =
                verdier(repo.hentUbehandledeIder(tidspunkt, limit = 10))

            repo.hentUbehandledeIder(tidspunktVedTeststart, limit = 10).let { ids ->
                verdier(ids) shouldBe listOf(
                    "samme-nøkkel-annet-topic",
                    "samme-nøkkel-partisjon-0",
                    "annen-nøkkel-offset-2",
                    "null-nøkkel-offset-5",
                    "null-nøkkel-offset-6",
                    "annen-nøkkel-offset-10",
                    "lavere-offset",
                )
                repo.hentUbehandledeIder(tidspunktVedTeststart, limit = 3) shouldBe ids.take(3)
            }

            val mottatt = mottattPerTestverdi.getValue("lavere-offset")
            val nesteForsøk = mottatt.plus(1, ChronoUnit.HOURS)
            val historikk = Forsøkshistorikk(
                forrigeForsøk = mottatt,
                nesteForsøk = nesteForsøk,
                antallForsøk = 2,
            )
            repo.lagreFeil(
                id = lavereOffsetId,
                feil = "midlertidig-feil",
                forbigående = true,
                forsøkshistorikk = historikk,
                kobling = null,
            )
            repo.lagreFeil(
                id = nullNøkkelOffset5Id,
                feil = "permanent-feil",
                forbigående = false,
                forsøkshistorikk = historikk,
                kobling = null,
            )

            verdierPå(nesteForsøk.minus(1, ChronoUnit.MICROS)) shouldBe listOf(
                "samme-nøkkel-annet-topic",
                "samme-nøkkel-partisjon-0",
                "annen-nøkkel-offset-2",
                "null-nøkkel-offset-6",
                "annen-nøkkel-offset-10",
            )
            verdierPå(nesteForsøk) shouldBe listOf(
                "samme-nøkkel-annet-topic",
                "samme-nøkkel-partisjon-0",
                "annen-nøkkel-offset-2",
                "null-nøkkel-offset-5",
                "null-nøkkel-offset-6",
                "annen-nøkkel-offset-10",
                "lavere-offset",
            )

            val lagret = tac.sessionFactory.withTransactionContext { context -> repo.hent(lavereOffsetId, context) }
            lagret shouldBe RåUtbetalingskvittering(
                id = lavereOffsetId,
                key = "samme",
                value = """{"simulering":true,"testverdi":"lavere-offset"}""",
                mottatt = mottatt,
                behandlet = null,
                sakId = null,
                utbetalingId = null,
                forsøkshistorikk = historikk,
            )
            repo.antallPermanenteFeil() shouldBe 1L
            repo.antallForbigåendeFeil(mottatt.minus(1, ChronoUnit.MICROS)) shouldBe 0L
            repo.antallForbigåendeFeil(mottatt) shouldBe 1L

            val jobb = tac.utbetalingContext.behandleUtbetalingskvitteringerJobb
            jobb.behandle()
            tac.clock.spolTil(nesteForsøk)
            jobb.behandle()

            val behandlet = tac.sessionFactory.withTransactionContext { context -> repo.hent(lavereOffsetId, context) }
            val behandletTid = requireNotNull(behandlet.behandlet)
            behandletTid.isBefore(nesteForsøk) shouldBe false
            behandlet.forsøkshistorikk shouldBe historikk
            verdierPå(nesteForsøk) shouldBe listOf("høyere-offset")
            repo.antallForbigåendeFeil(mottatt) shouldBe 0L

            jobb.behandle()
            val nyereBehandlet = tac.sessionFactory.withTransactionContext { context -> repo.hent(høyereOffsetId, context) }
            requireNotNull(nyereBehandlet.behandlet).isBefore(behandletTid) shouldBe false

            val feiltilstandEtterBehandling = tac.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                        select behandlet_feil, forbigaaende::text as forbigaaende
                        from utbetalingskvittering
                        where id = :id
                        """,
                        "id" to lavereOffsetId.toString(),
                    ).map {
                        it.stringOrNull("behandlet_feil") to it.stringOrNull("forbigaaende")
                    }.asSingle,
                )
            }
            feiltilstandEtterBehandling shouldBe (null to null)
        }
    }
}
