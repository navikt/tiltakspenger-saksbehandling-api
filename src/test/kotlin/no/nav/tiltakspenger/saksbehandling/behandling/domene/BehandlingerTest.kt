package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

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
}
