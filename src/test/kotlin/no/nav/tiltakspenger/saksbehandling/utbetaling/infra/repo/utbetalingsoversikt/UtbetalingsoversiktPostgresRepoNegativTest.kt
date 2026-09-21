package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktId
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import java.time.LocalDate

/**
 * Radmappingen i repoet hviler på disse constraintene, så testen viser at de finnes.
 * Radene settes inn med direkte SQL fordi repoet ikke kan skrive dem.
 */
class UtbetalingsoversiktPostgresRepoNegativTest {
    @Test
    fun `fremmednøkkelen krever en eksisterende sak`() {
        withTestApplicationContextAndPostgres { tac ->
            shouldThrow<PSQLException> { settInn(tac, sakId = SakId.random()) }.message shouldContain "utbetalingsoversikt_sak_id_fkey"
        }
    }

    @Test
    fun `constraintene avviser ugyldige rader`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen)

            shouldThrow<PSQLException> { settInn(tac, sak.id, periodetype = "UKJENT") }.message shouldContain "oppslag_periodetype"
            shouldThrow<PSQLException> { settInn(tac, sak.id, resultat = "UKJENT") }.message shouldContain "resultat"
            shouldThrow<PSQLException> { settInn(tac, sak.id, antallFeilPåRad = -1) }.message shouldContain "antall_feil_på_rad"
            shouldThrow<PSQLException> {
                settInn(tac, sak.id, oppslagFom = LocalDate.parse("2025-02-01"), oppslagTom = LocalDate.parse("2025-01-31"))
            }.message shouldContain "utbetalingsoversikt_oppslagsperiode"
            shouldThrow<PSQLException> { settInn(tac, sak.id, nesteOppslag = "now() - interval '1 second'") }.message shouldContain "utbetalingsoversikt_neste_oppslag"
        }
    }

    @Test
    fun `vellykket rad må ha utbetalinger og kan ikke ha feiltype`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen)

            shouldThrow<PSQLException> { settInn(tac, sak.id, utbetalinger = null) }.message shouldContain "utbetalingsoversikt_resultat_innhold"
            shouldThrow<PSQLException> { settInn(tac, sak.id, feiltype = "TJENESTEFEIL") }.message shouldContain "utbetalingsoversikt_resultat_innhold"
            shouldThrow<PSQLException> { settInn(tac, sak.id, antallFeilPåRad = 1) }.message shouldContain "utbetalingsoversikt_resultat_innhold"
        }
    }

    @Test
    fun `feilet rad må ha feiltype og kan ikke ha utbetalinger`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen)

            shouldThrow<PSQLException> {
                settInn(tac, sak.id, resultat = "FEILET", feiltype = null, utbetalinger = null)
            }.message shouldContain "utbetalingsoversikt_resultat_innhold"
            shouldThrow<PSQLException> {
                settInn(tac, sak.id, resultat = "FEILET", feiltype = "TJENESTEFEIL", antallFeilPåRad = 1)
            }.message shouldContain "utbetalingsoversikt_resultat_innhold"
            shouldThrow<PSQLException> {
                settInn(tac, sak.id, resultat = "FEILET", feiltype = "TJENESTEFEIL", utbetalinger = null, antallFeilPåRad = 0)
            }.message shouldContain "utbetalingsoversikt_resultat_innhold"
            shouldThrow<PSQLException> {
                settInn(tac, sak.id, resultat = "FEILET", feiltype = "UKJENT", utbetalinger = null, antallFeilPåRad = 1)
            }.message shouldContain "feiltype"
        }
    }

    private fun settInn(
        tac: TestApplicationContext,
        sakId: SakId,
        periodetype: String = "YTELSESPERIODE",
        resultat: String = "VELLYKKET",
        feiltype: String? = null,
        utbetalinger: String? = """{"utbetalinger":[]}""",
        oppslagFom: LocalDate = LocalDate.parse("2025-01-01"),
        oppslagTom: LocalDate = LocalDate.parse("2025-01-31"),
        antallFeilPåRad: Int = 0,
        nesteOppslag: String = "now()",
    ) {
        (tac.sessionFactory as PostgresSessionFactory).withSession {
            it.run(
                queryOf(
                    """
                    INSERT INTO utbetalingsoversikt (
                        id, sak_id, hentet, oppslag_fom, oppslag_tom, oppslag_periodetype,
                        resultat, feiltype, utbetalinger, metadata, neste_oppslag, antall_feil_på_rad
                    ) VALUES (
                        :id, :sak_id, now(), :oppslag_fom, :oppslag_tom, :periodetype,
                        :resultat, :feiltype, :utbetalinger::jsonb, '{}'::jsonb, $nesteOppslag, :antall_feil_pa_rad
                    )
                    """.trimIndent(),
                    mapOf(
                        "id" to UtbetalingsoversiktId.random().toString(),
                        "sak_id" to sakId.toString(),
                        "periodetype" to periodetype,
                        "resultat" to resultat,
                        "feiltype" to feiltype,
                        "utbetalinger" to utbetalinger,
                        "oppslag_fom" to oppslagFom,
                        "oppslag_tom" to oppslagTom,
                        "antall_feil_pa_rad" to antallFeilPåRad,
                    ),
                ).asUpdate,
            )
        }
    }
}
