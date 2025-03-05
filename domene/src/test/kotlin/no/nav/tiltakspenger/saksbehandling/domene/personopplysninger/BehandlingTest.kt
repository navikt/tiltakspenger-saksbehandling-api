package no.nav.tiltakspenger.saksbehandling.domene.personopplysninger

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Avbrutt
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingTest {

    @Test
    fun `kan avbryte en behandling`() {
        val behandling = ObjectMother.nyBehandling()
        val avbruttBehandling = behandling.avbryt(
            avbruttAv = ObjectMother.saksbehandler(),
            begrunnelse = "begrunnelse",
            tidspunkt = førsteNovember24,
        )

        avbruttBehandling.erAvsluttet shouldBe true
        avbruttBehandling.avbrutt.let {
            it shouldNotBe null
            it!!.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            it.begrunnelse shouldBe "begrunnelse"
            it.tidspunkt shouldBe førsteNovember24
        }
        avbruttBehandling.søknad!!.avbrutt shouldNotBe null
        avbruttBehandling.avbrutt shouldBe avbruttBehandling.søknad!!.avbrutt
        avbruttBehandling.status shouldBe Behandlingsstatus.AVBRUTT
    }

    @Test
    fun `kan ikke avbryte en avbrutt behandling`() {
        val avbruttBehandling = ObjectMother.nyBehandling(
            status = Behandlingsstatus.AVBRUTT,
            avbrutt = Avbrutt(
                tidspunkt = førsteNovember24,
                saksbehandler = "navident",
                begrunnelse = "skal få exception",
            ),
        )

        assertThrows<IllegalArgumentException> {
            avbruttBehandling.avbryt(
                avbruttAv = ObjectMother.saksbehandler(),
                begrunnelse = "begrunnelse",
                tidspunkt = førsteNovember24,
            )
        }
    }
}
