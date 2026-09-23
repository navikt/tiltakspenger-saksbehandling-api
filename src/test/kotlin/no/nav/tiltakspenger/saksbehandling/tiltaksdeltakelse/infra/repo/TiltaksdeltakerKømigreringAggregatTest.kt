package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.KometTiltakHendelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.TiltaksdeltakerKometConsumer
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Bygger saker og hendelser gjennom prodstiene, og gjenskaper deretter den historiske køtilstanden med SQL.
 * Direkte SQL er nødvendig fordi den nye consumeren alltid setter markør og den gamle jobbens kvittering er fjernet.
 * Kjører den faktiske migreringsfilen mot historiske data etter at testdatabasen er migrert.
 * DDL-en i migreringen er idempotent, så hele fila kan kjøres på nytt, og det er flyttingen av køen som testes her.
 */
class TiltaksdeltakerKømigreringAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `flytter bare ubehandlede hendelser og beholder nyeste tidspunkt uten å endre historikken`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val ubehandlet = opprettDeltaker(tac)
            val behandlet = opprettDeltaker(tac)
            val nyereMarkør = opprettDeltaker(tac)
            val eldreMarkør = opprettDeltaker(tac)
            val utenHendelser = opprettDeltaker(tac)
            val tidspunkt = nå(tac.clock).withNano(0)
            tac.lagreHistoriskHendelse(ubehandlet, tidspunkt.minusMinutes(40))
            tac.lagreHistoriskHendelse(ubehandlet, tidspunkt.minusMinutes(20))
            tac.lagreHistoriskHendelse(ubehandlet, tidspunkt.minusMinutes(10), erBehandlet = true)
            tac.lagreHistoriskHendelse(behandlet, tidspunkt.minusMinutes(30), erBehandlet = true)
            tac.lagreHistoriskHendelse(nyereMarkør, tidspunkt.minusMinutes(20))
            tac.lagreHistoriskHendelse(eldreMarkør, tidspunkt.minusMinutes(20))
            val repo = tac.tiltakContext.tiltaksdeltakerRepo
            listOf(ubehandlet, behandlet, nyereMarkør, eldreMarkør).forEach { deltaker ->
                val markør = repo.hentTiltaksdeltaker(deltaker.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldNotBeNull()
                repo.markerEndringSomBehandlet(deltaker.id, markør)
            }
            repo.registrerUbehandletEndring(nyereMarkør.id, nyereMarkør.sakId, tidspunkt.minusMinutes(5))
            repo.registrerUbehandletEndring(eldreMarkør.id, eldreMarkør.sakId, tidspunkt.minusMinutes(50))
            val historikkFør = tac.hentHistorikk()
            val migrering = javaClass.getResource(
                "/db/migration/V253__tiltaksdeltaker_sak_id_og_siste_ubehandlet_endring.sql",
            ).shouldNotBeNull().readText()

            tac.sessionFactory.withSession { session -> session.run(sqlQuery(migrering).asExecute) }

            repo.hentTiltaksdeltaker(ubehandlet.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe tidspunkt.minusMinutes(20)
            repo.hentTiltaksdeltaker(behandlet.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            repo.hentTiltaksdeltaker(nyereMarkør.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe tidspunkt.minusMinutes(5)
            repo.hentTiltaksdeltaker(eldreMarkør.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe tidspunkt.minusMinutes(20)
            repo.hentTiltaksdeltaker(utenHendelser.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            tac.hentHistorikk() shouldBe historikkFør

            tac.oppdatertTiltaksdeltakelseJobb.håndterUbehandledeEndringer()

            repo.hentTiltaksdeltaker(ubehandlet.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            repo.hentTiltaksdeltaker(eldreMarkør.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldBeNull()
            repo.hentTiltaksdeltaker(nyereMarkør.eksternId).shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe tidspunkt.minusMinutes(5)
            tac.hentHistorikk() shouldBe historikkFør
        }
    }

    private suspend fun ApplicationTestBuilder.opprettDeltaker(tac: TestApplicationContextMedPostgres): Tiltaksdeltaker {
        val deltakelse = tac.tiltaksdeltakelse().copy(eksternDeltakelseId = UUID.randomUUID().toString())
        opprettSakOgSøknad(tac = tac, tiltaksdeltakelse = deltakelse)
        return tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(deltakelse.eksternDeltakelseId).shouldNotBeNull()
    }

    private fun TestApplicationContextMedPostgres.lagreHistoriskHendelse(
        deltaker: Tiltaksdeltaker,
        tidspunkt: LocalDateTime,
        erBehandlet: Boolean = false,
    ) {
        val melding = KometTiltakHendelseDTO(
            id = UUID.fromString(deltaker.eksternId),
            startDato = 5.januar(2025),
            sluttDato = 5.mai(2025),
            status = KometTiltakHendelseDTO.DeltakerStatusDto(KometDeltakerStatusTypeDTO.DELTAR),
            dagerPerUke = 2.0F,
            prosentStilling = 50.0F,
        )
        val hendelseId = TiltaksdeltakerKometConsumer.consume(
            deltakerId = melding.id,
            melding = serialize(melding),
            tiltaksdeltakerRepo = tiltakContext.tiltaksdeltakerRepo,
            søknadRepo = søknadContext.søknadRepo,
            tiltaksdeltakerHendelsePostgresRepo = tiltaksdeltakerHendelsePostgresRepo,
            clock = clock,
        ).shouldNotBeNull()
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        UPDATE tiltaksdeltaker_kafka
                        SET sist_oppdatert = :tidspunkt, behandlet_tidspunkt = :behandlet
                        WHERE hendelse_id = :hendelse_id
                    """.trimIndent(),
                    "tidspunkt" to tidspunkt,
                    "behandlet" to if (erBehandlet) tidspunkt else null,
                    "hendelse_id" to hendelseId.toString(),
                ).asUpdate,
            )
        }
    }

    private fun TestApplicationContextMedPostgres.hentHistorikk(): List<String> = sessionFactory.withSession { session ->
        session.run(
            sqlQuery(
                """
                    SELECT row_to_json(h)::text AS historikk
                    FROM tiltaksdeltaker_kafka h
                    ORDER BY hendelse_id
                """.trimIndent(),
            ).map { it.string("historikk") }.asList,
        )
    }
}
