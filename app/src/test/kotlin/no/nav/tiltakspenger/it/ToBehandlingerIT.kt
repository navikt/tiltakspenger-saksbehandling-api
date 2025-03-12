package no.nav.tiltakspenger.it

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingIverksatt
import org.junit.jupiter.api.Test

class ToBehandlingerIT {

    @Test
    fun `kan iverksette 2 behandlinger`() {
        runTest {
            with(TestApplicationContext()) {
                val fnr = Fnr.random()
                val førsteVirkningsperiode = Periode(1.mars(2025), 15.mars(2025))
                val andreVirkningsperiode = Periode(1.mars(2025), 15.mars(2025))

                val sakFørsteBehandling = this.førstegangsbehandlingIverksatt(
                    fnr = fnr,
                    periode = førsteVirkningsperiode,
                )

                sakFørsteBehandling.let {
                    it.soknader.size shouldBe 1
                    it.behandlinger.size shouldBe 1
                    // TODO - vi må nok gjøre mer for at denne skal trigges
                    it.meldeperiodeKjeder.size shouldBe 1
                }

                val sakAndreBehandling = this.førstegangsbehandlingIverksatt(
                    fnr = fnr,
                    periode = andreVirkningsperiode,
                )

                sakAndreBehandling.let {
                    it.soknader.size shouldBe 2
                    it.behandlinger.size shouldBe 2
                    it.meldeperiodeKjeder.size shouldBe 2
                }
            }
        }
    }
}
