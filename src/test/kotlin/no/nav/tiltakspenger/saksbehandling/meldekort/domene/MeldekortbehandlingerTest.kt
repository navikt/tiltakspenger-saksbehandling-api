package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortbehandlingerTest {

    @Test
    fun `flere meldekortbehandlinger kan vaere aapne samtidig`() {
        val sakId = SakId.random()
        val meldekortbehandlinger = shouldNotThrowAny {
            Meldekortbehandlinger(
                verdi = listOf(
                    ObjectMother.meldekortUnderBehandling(sakId = sakId, opprettet = 1.januar(2025).atStartOfDay()),
                    ObjectMother.meldekortUnderBehandling(sakId = sakId, opprettet = 2.januar(2025).atStartOfDay()),
                ),
            )
        }

        meldekortbehandlinger.åpneMeldekortbehandlinger.size shouldBe 2
        meldekortbehandlinger.harÅpenBehandling shouldBe true
        meldekortbehandlinger.meldekortbehandlingerUnderBehandling.size shouldBe 2
    }

    @Test
    fun `ingen aapne meldekortbehandlinger`() {
        val meldekortbehandlinger = Meldekortbehandlinger(verdi = emptyList())

        meldekortbehandlinger.åpneMeldekortbehandlinger.size shouldBe 0
        meldekortbehandlinger.harÅpenBehandling shouldBe false
    }
}
