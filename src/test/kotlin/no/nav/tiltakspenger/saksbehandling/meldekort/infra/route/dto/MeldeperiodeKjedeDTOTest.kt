package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import org.junit.jupiter.api.Test

/**
 * Testene kjører med testklokka som står 1. mai 2025.
 */
class MeldeperiodeKjedeDTOTest {

    @Test
    fun `kjede uten åpen behandling som har startet og gir rett kan behandles`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            sak.tilMeldeperiodeKjederDTO(tac.clock).first().also {
                it.kanBehandles shouldBe true
                it.kanIkkeBehandlesGrunn shouldBe null
            }
        }
    }

    @Test
    fun `kjede med åpen behandling kan ikke behandles`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )
            val kjedeMedÅpenBehandling = sak.meldeperiodeKjeder.first().kjedeId

            val (oppdatertSak) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeMedÅpenBehandling,
            )!!

            oppdatertSak.tilMeldeperiodeKjederDTO(tac.clock)
                .single { it.id == kjedeMedÅpenBehandling.toString() }
                .also {
                    it.kanBehandles shouldBe false
                    it.kanIkkeBehandlesGrunn shouldBe KanIkkeBehandlesGrunnDTO.HAR_ÅPEN_BEHANDLING
                }
        }
    }

    @Test
    fun `kjede som ikke har startet kan ikke behandles`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            sak.tilMeldeperiodeKjederDTO(tac.clock).last().also {
                it.periode.fraOgMed shouldBe 26.mai(2025).toString()
                it.kanBehandles shouldBe false
                it.kanIkkeBehandlesGrunn shouldBe KanIkkeBehandlesGrunnDTO.MELDEPERIODEN_HAR_IKKE_STARTET
            }
        }
    }

    @Test
    fun `kjede uten dager som gir rett kan ikke behandles`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode: Periode = 1 til 31.januar(2025)
            val (sak, _, rammevedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (oppdatertSak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(1 til 1.januar(2025)),
            )

            val kjeder = oppdatertSak.tilMeldeperiodeKjederDTO(tac.clock)

            kjeder[0].also {
                it.kanBehandles shouldBe true
                it.kanIkkeBehandlesGrunn shouldBe null
            }
            kjeder[1].also {
                it.kanBehandles shouldBe false
                it.kanIkkeBehandlesGrunn shouldBe KanIkkeBehandlesGrunnDTO.INGEN_DAGER_GIR_RETT
            }
        }
    }
}
