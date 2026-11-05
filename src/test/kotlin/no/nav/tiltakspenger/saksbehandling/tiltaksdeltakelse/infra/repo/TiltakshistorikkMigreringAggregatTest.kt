package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Gjenskaper rader jobben tidligere skrev til `tiltaksdeltaker_endring` med SQL, og kjører den faktiske migreringsfila på nytt.
 * DDL-en i migreringen er idempotent, så det er flyttingen av radene som testes her.
 */
class TiltakshistorikkMigreringAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `flytter nå-tilstandene fra tiltakshistorikk og beholder hendelsene fra andre kilder`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val tidspunkt = nå(tac.clock).withNano(0)
            val behandlingId = sak.rammebehandlinger.single().id.toString()
            val medBehandling = tac.lagreHistoriskRad(
                sak = sak,
                kilde = "Tiltakshistorikk",
                verdi = """{"versjon":1}""",
                endring = """[{"type":"FORLENGELSE"}]""",
                behandlingId = behandlingId,
                opprettet = tidspunkt.minusMinutes(30),
                behandletTidspunkt = tidspunkt.minusMinutes(20),
            )
            val medOppgave = tac.lagreHistoriskRad(
                sak = sak,
                kilde = "Tiltakshistorikk",
                verdi = """{"versjon":2}""",
                oppgaveId = "oppgave-1",
                opprettet = tidspunkt.minusMinutes(10),
            )
            tac.lagreHistoriskRad(sak = sak, kilde = "Tiltakshistorikk", verdi = null, opprettet = tidspunkt.minusMinutes(5))
            val komet = tac.lagreHistoriskRad(sak = sak, kilde = "Komet", verdi = """{"komet":true}""", opprettet = tidspunkt.minusMinutes(40))
            val migrering = javaClass.getResource("/db/migration/V274__tiltakshistorikk.sql").shouldNotBeNull().readText()

            tac.sessionFactory.withSession { session -> session.run(sqlQuery(migrering).asExecute) }

            val tiltak = sak.søknader.single().tiltak.shouldNotBeNull()
            val historikk = tac.sessionFactory.hentTiltakshistorikk(tiltak.tiltaksdeltakerId)
            historikk shouldHaveSize 2
            historikk[0].also {
                it.id shouldBe medBehandling.replace("tiltaksdeltakerhendelse_", "tiltakshistorikkinnslag_")
                it.sakId shouldBe sak.id.toString()
                it.eksternDeltakerId shouldBe tiltak.id
                it.innhentet shouldBe tidspunkt.minusMinutes(20)
                it.verdi shouldEqualJson """{"versjon":1}"""
                it.endring.shouldNotBeNull() shouldEqualJson """[{"type":"FORLENGELSE"}]"""
                it.behandlingId shouldBe behandlingId
                it.oppgaveId.shouldBeNull()
            }
            historikk[1].also {
                it.id shouldBe medOppgave.replace("tiltaksdeltakerhendelse_", "tiltakshistorikkinnslag_")
                it.innhentet shouldBe tidspunkt.minusMinutes(10)
                it.verdi shouldEqualJson """{"versjon":2}"""
                it.endring.shouldBeNull()
                it.behandlingId.shouldBeNull()
                it.oppgaveId shouldBe "oppgave-1"
            }
            tac.sessionFactory.hentTiltaksdeltakerEndringerForEksternId(tiltak.id).map { it.id to it.kilde } shouldBe
                listOf(komet to TiltaksdeltakerHendelseKilde.Komet)
        }
    }

    /** Skriver en rad slik den gamle jobben og consumerne gjorde, og returnerer id-en. */
    private fun TestApplicationContextMedPostgres.lagreHistoriskRad(
        sak: Sak,
        kilde: String,
        verdi: String?,
        opprettet: LocalDateTime,
        endring: String? = null,
        behandlingId: String? = null,
        oppgaveId: String? = null,
        behandletTidspunkt: LocalDateTime? = null,
    ): String {
        val tiltak = sak.søknader.single().tiltak.shouldNotBeNull()
        val id = TiltaksdeltakerHendelseId.random().toString()
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        INSERT INTO tiltaksdeltaker_endring (
                            id, ekstern_deltaker_id, tiltaksdeltaker_id, sak_id, kilde, verdi, endring,
                            behandling_id, oppgave_id, opprettet, behandlet_tidspunkt
                        ) VALUES (
                            :id, :ekstern_deltaker_id, :tiltaksdeltaker_id, :sak_id, :kilde, CAST(:verdi AS jsonb), CAST(:endring AS jsonb),
                            :behandling_id, :oppgave_id, :opprettet, :behandlet_tidspunkt
                        )
                    """.trimIndent(),
                    "id" to id,
                    "ekstern_deltaker_id" to tiltak.id,
                    "tiltaksdeltaker_id" to tiltak.tiltaksdeltakerId.toString(),
                    "sak_id" to sak.id.toString(),
                    "kilde" to kilde,
                    "verdi" to verdi,
                    "endring" to endring,
                    "behandling_id" to behandlingId,
                    "oppgave_id" to oppgaveId,
                    "opprettet" to opprettet,
                    "behandlet_tidspunkt" to behandletTidspunkt,
                ).asUpdate,
            )
        }
        return id
    }
}
