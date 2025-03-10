package no.nav.tiltakspenger.domene.behandling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Avbrutt
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingerTest {

    @Test
    fun `henter alle åpne behandlinger`() {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val åpenBehandling = ObjectMother.nyBehandling(sakId = sakId, fnr = fnr)
        val vedtattBehandling = ObjectMother.nyVedtattBehandling(sakId = sakId, fnr = fnr)
        val avbruttBehandling = ObjectMother.nyBehandling(
            fnr = fnr,
            sakId = sakId,
            status = Behandlingsstatus.AVBRUTT,
            avbrutt = Avbrutt(førsteNovember24, "aaa", "bbb"),
            opprettet = LocalDateTime.now(),
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
