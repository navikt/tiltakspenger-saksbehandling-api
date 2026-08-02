package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException

/**
 * Negative tester for [UtbetalingPostgresRepo]: databasetilstander prodkoden ikke kan skrive.
 *
 * Testen muterer databasen direkte, og det er selve poenget.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class UtbetalingPostgresRepoNegativTest {

    /**
     * En utbetaling hører til enten et rammevedtak eller et meldekortvedtak, aldri begge og aldri ingen.
     * `tilUtbetaling` bruker `rammevedtakId!!` på den garantien, og garantien er ikke sterkere enn check-constrainten som holder den i live.
     * Testen verifiserer at constrainten faktisk finnes, ved å forsøke oppdateringen den skal stoppe.
     */
    @Test
    fun `en utbetaling må peke på enten rammevedtak eller meldekortvedtak`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            // Utbetalingsraden oppstår først når et meldekort iverksettes.
            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )!!

            val forsøk = shouldThrow<PSQLException> {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            "update utbetaling set rammevedtak_id = null, meldekortvedtak_id = null where sak_id = :sakId",
                            mapOf("sakId" to sak.id.toString()),
                        ).asUpdate,
                    )
                }
            }

            forsøk.message shouldContain "rammevedtak_eller_meldekortvedtak"
        }
    }
}
