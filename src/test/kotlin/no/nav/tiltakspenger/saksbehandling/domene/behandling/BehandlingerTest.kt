package no.nav.tiltakspenger.saksbehandling.domene.behandling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingerTest {

    @Test
    fun `henter alle åpne behandlinger`() {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val åpenBehandling = ObjectMother.nyOpprettetSøknadsbehandling(sakId = sakId, fnr = fnr)
        val vedtattBehandling = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = Periode(1.januar(2025), 10.januar(2025)),
        )
        val avbruttBehandling = ObjectMother.nyAvbruttSøknadsbehandling(
            fnr = fnr,
            sakId = sakId,
        )
        val åpenOgAvbrutt = Behandlinger(listOf(åpenBehandling, avbruttBehandling))
        val vedtattOgAvbrutt = Behandlinger(listOf(vedtattBehandling, avbruttBehandling))

        val actualÅpenOgAvbrutt = åpenOgAvbrutt.hentÅpneBehandlinger()
        actualÅpenOgAvbrutt.size shouldBe 1
        actualÅpenOgAvbrutt.first() shouldBe åpenBehandling

        val actualVedtattOgAvbrutt = vedtattOgAvbrutt.hentÅpneBehandlinger()
        actualVedtattOgAvbrutt.size shouldBe 0
    }

    @Test
    fun `kan ikke ha overlappende virkningsperioder`() {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val b1 = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = Periode(1.januar(2025), 10.januar(2025)),
        )
        val b2 = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = Periode(1.januar(2025), 10.januar(2025)),
        )

        assertThrows<IllegalArgumentException> {
            Behandlinger(listOf(b1, b2))
        }.message shouldBe "Førstegangsbehandlinger kan ikke ha overlappende virkningsperiode"
    }

    @Test
    fun `søknadsbehandlinger kan ikke tilstøte hverandre (må ha hull i mellom)`() {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val b1 = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = Periode(1.januar(2025), 10.januar(2025)),
        )
        val b2 = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = Periode(11.januar(2025), 20.januar(2025)),
        )

        assertThrows<IllegalArgumentException> {
            Behandlinger(listOf(b1, b2))
        }.message shouldBe "Førstegangsbehandlinger kan ikke tilstøte hverandre (må ha hull i mellom)"
    }
}
