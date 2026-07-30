package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import kotliquery.queryOf
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * Negative tester for [MeldekortvedtakPostgresRepo]: databasetilstander prodkoden ikke kan skrive.
 *
 * Testene muterer databasen direkte, og det er selve poenget.
 * De verifiserer at repoet oppdager korrupt data i stedet for å mappe den til en ugyldig domenemodell.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class MeldekortvedtakPostgresRepoNegativTest {

    @Test
    fun `kaster når meldekortbehandlingen bak vedtaket ikke er behandlet`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val (_, meldekortvedtak, meldekortbehandling) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )!!

            // Ruller meldekortbehandlingen tilbake til UNDER_BEHANDLING.
            // Et iverksatt meldekortvedtak kan aldri peke på en behandling i den statusen.
            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update meldekortbehandling set status = 'UNDER_BEHANDLING' where id = :id",
                        mapOf("id" to meldekortbehandling.id.toString()),
                    ).asUpdate,
                )
            }

            shouldThrowWithMessage<IllegalArgumentException>(
                "Meldekortet ${meldekortbehandling.id} på meldekortvedtak ${meldekortvedtak.id} er ikke et behandlet meldekort",
            ) {
                tac.utbetalingContext.meldekortvedtakRepo.hentForVedtakId(meldekortvedtak.id)
            }
        }
    }
}
