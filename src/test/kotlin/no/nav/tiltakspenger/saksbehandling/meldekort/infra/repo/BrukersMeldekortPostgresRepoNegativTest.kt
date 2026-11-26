package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import org.junit.jupiter.api.Test

/**
 * Negative tester for [BrukersMeldekortPostgresRepo]: tilstander prodkoden ikke skal havne i.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class BrukersMeldekortPostgresRepoNegativTest {

    /**
     * Et meldekort kan bare behandles av én iverksatt behandling, siden en kjede bare kan ha én åpen behandling om gangen.
     * Markeres det likevel på nytt, skal repoet kaste i stedet for å overskrive markeringen.
     */
    @Test
    fun `kaster og beholder markeringen når et meldekort som allerede er behandlet markeres på nytt`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1 til 31.januar(2025)),
            )
            val meldeperiode = sak.meldeperiodeKjeder[0].hentSisteMeldeperiode()
            val (_, brukersMeldekort) = mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = meldeperiode.id,
                sakId = sak.id,
                dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort),
            )
            val (_, _, iverksatt) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldeperiode.kjedeId,
            )!!
            val repo = tac.meldekortContext.brukersMeldekortRepo

            shouldThrowWithMessage<IllegalStateException>(
                "Kunne ikke markere brukers meldekort som behandlet av ${iverksatt.id}, siden bare 0 av 1 var ubehandlet. sakId: ${sak.id}, meldekort: [${brukersMeldekort!!.id}]",
            ) {
                repo.markerSomBehandlet(iverksatt, null)
            }
            repo.hentForMeldekortId(brukersMeldekort.id)!!.behandletAv shouldBe iverksatt.id
        }
    }
}
