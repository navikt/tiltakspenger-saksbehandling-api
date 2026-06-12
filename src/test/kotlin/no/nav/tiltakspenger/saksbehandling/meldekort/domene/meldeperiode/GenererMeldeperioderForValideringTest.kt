package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode

import arrow.core.left
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class GenererMeldeperioderForValideringTest {

    @Test
    fun `genererer meldeperioder for en ikke-vedtatt behandling med vedtaksperiode`() {
        val (sak, behandling) = ObjectMother.sakMedOpprettetBehandling()

        val actual = sak.genererMeldeperioderForValidering(behandling, fixedClock)

        actual.isRight() shouldBe true
        actual.getOrNull()!!.isNotEmpty() shouldBe true
    }

    @Test
    fun `behandling uten vedtaksperiode gir BehandlingManglerVedtaksperiode`() {
        val (sak, _) = ObjectMother.sakMedOpprettetBehandling()
        // En nyopprettet søknadsbehandling har ikke valgt resultat enda, og dermed ingen vedtaksperiode.
        val behandlingUtenVedtaksperiode = ObjectMother.nyOpprettetSøknadsbehandling()
        behandlingUtenVedtaksperiode.vedtaksperiode shouldBe null

        sak.genererMeldeperioderForValidering(behandlingUtenVedtaksperiode, fixedClock) shouldBe
            GenererMeldeperioderFeil.BehandlingManglerVedtaksperiode.left()
    }

    @Test
    fun `vedtatt behandling gir FeilKallForVedtattBehandling`() {
        val (sak, _) = ObjectMother.sakMedOpprettetBehandling()
        // For vedtatte behandlinger skal meldeperioder genereres ut fra vedtak på saken, ikke behandlingen.
        val vedtattBehandling = ObjectMother.nyVedtattSøknadsbehandling()

        sak.genererMeldeperioderForValidering(vedtattBehandling, fixedClock) shouldBe
            GenererMeldeperioderFeil.FeilKallForVedtattBehandling.left()
    }
}
