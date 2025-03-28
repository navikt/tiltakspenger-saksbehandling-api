package no.nav.tiltakspenger.saksbehandling.domene.søknad

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SøknadTest {

    @Test
    fun `avbryter en søknad`() {
        val søknad = ObjectMother.nySøknad()
        val avbruttSøknad = søknad.avbryt(ObjectMother.saksbehandler(), "jeg avbryter søknad", førsteNovember24)

        avbruttSøknad.erAvbrutt shouldBe true
        avbruttSøknad.avbrutt.let {
            it shouldNotBe null
            it!!.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            it.begrunnelse shouldBe "jeg avbryter søknad"
            it.tidspunkt shouldBe førsteNovember24
        }
    }

    @Test
    fun `kaster exception dersom man prøver å avbryte en avbrutt søknad`() {
        val avbruttSøknad = ObjectMother.nySøknad(
            avbrutt = no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt(
                tidspunkt = førsteNovember24,
                saksbehandler = "navident",
                begrunnelse = "skal få exception",
            ),
        )

        assertThrows<IllegalStateException> {
            avbruttSøknad.avbryt(ObjectMother.saksbehandler(), "jeg avbryter søknad", førsteNovember24)
        }
    }
}
