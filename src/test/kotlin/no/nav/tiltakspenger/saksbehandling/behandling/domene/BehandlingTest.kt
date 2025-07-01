package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingTest {

    @Test
    fun `kan avbryte en behandling`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
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
        avbruttBehandling.søknad.avbrutt shouldNotBe null
        avbruttBehandling.avbrutt shouldBe avbruttBehandling.søknad.avbrutt
        avbruttBehandling.status shouldBe Behandlingsstatus.AVBRUTT
    }

    @Test
    fun `kan avbryte en automatisk opprettet behandling`() {
        val behandling = ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()
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
        avbruttBehandling.søknad.avbrutt shouldNotBe null
        avbruttBehandling.avbrutt shouldBe avbruttBehandling.søknad.avbrutt
        avbruttBehandling.status shouldBe Behandlingsstatus.AVBRUTT
    }

    @Test
    fun `kan ikke avbryte en avbrutt behandling`() {
        val avbruttBehandling = ObjectMother.nyAvbruttSøknadsbehandling(
            tidspunkt = førsteNovember24,
            avbruttAv = ObjectMother.saksbehandler(navIdent = "navident"),
            begrunnelse = "skal få exception",
        )

        assertThrows<IllegalArgumentException> {
            avbruttBehandling.avbryt(
                avbruttAv = ObjectMother.saksbehandler(),
                begrunnelse = "begrunnelse",
                tidspunkt = førsteNovember24,
            )
        }
    }

    @Nested
    inner class TaBehandling {
        @Test
        fun `en saksbehandler kan ta en ikke tildelt behandling`() {
            val behandling = ObjectMother.nyAutomatiskSøknadsbehandlingManuellBehandling()
            val saksbehandler = ObjectMother.saksbehandler()

            val taBehandling = behandling.taBehandling(saksbehandler)

            taBehandling.saksbehandler shouldBe saksbehandler.navIdent
            taBehandling.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
        }

        @Test
        fun `en beslutter kan ta behandlingen`() {
            val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning()
            val beslutter = ObjectMother.beslutter()
            val taBehandling = behandling.taBehandling(beslutter)

            taBehandling.beslutter shouldBe beslutter.navIdent
            taBehandling.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
        }
    }

    @Nested
    inner class Overta {
        @Test
        fun `en saksbehandler kan overta behandlingen`() {
            val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
            val nySaksbehandler = ObjectMother.saksbehandler("nyNavIdent")
            val overtaBehandling = behandling.overta(saksbehandler = nySaksbehandler, clock = fixedClock)

            behandling.saksbehandler.shouldNotBe(nySaksbehandler.navIdent)
            overtaBehandling.getOrFail().saksbehandler shouldBe nySaksbehandler.navIdent
        }

        @Test
        fun `en beslutter kan overta behandlingen`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val nyBeslutter = ObjectMother.beslutter("nyNavIdent")
            val overtaBehandling = behandling.overta(saksbehandler = nyBeslutter, clock = fixedClock)

            behandling.beslutter.shouldNotBe(nyBeslutter.navIdent)
            overtaBehandling.getOrFail().beslutter shouldBe nyBeslutter.navIdent
        }
    }

    @Nested
    inner class BehandlingsresultatTest {
        @Test
        fun `kaster exception dersom resultat er avslag uten avslagsgrunner`() {
            assertThrows<IllegalArgumentException> {
                ObjectMother.nySøknadsbehandlingKlarTilBeslutning(
                    resultat = SøknadsbehandlingType.AVSLAG,
                    avslagsgrunner = null,
                )
            }
        }

        @Test
        fun `kaster exception dersom resultat er innvilgelse med avslagsgrunner`() {
            assertThrows<IllegalArgumentException> {
                ObjectMother.nySøknadsbehandlingKlarTilBeslutning(
                    resultat = SøknadsbehandlingType.INNVILGELSE,
                    avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
                )
            }
        }
    }

    @Nested
    inner class ValiderKanOppdatereTest {
        @Test
        fun `kaster tilgangsexception dersom saksbehandler ikke er saksbehandler`() {
            assertThrows<TilgangException> {
                val saksbehandler = ObjectMother.saksbehandler(roller = Saksbehandlerroller(emptyList()))
                val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
                behandling.validerKanOppdatere(saksbehandler, "en error msg")
            }
        }

        @Test
        fun `returnerer left dersom utdøvende saksbehandler ikke eier behandlingen`() {
            val søknadsbehandling = ObjectMother.nyOpprettetSøknadsbehandling()
            val ikkeUtdøvendeSaksbehandler = ObjectMother.saksbehandler(navIdent = "ikkeUtdøvendeSaksbehandler")

            søknadsbehandling.validerKanOppdatere(ikkeUtdøvendeSaksbehandler, "en error msg").isLeft() shouldBe true
        }

        @Test
        fun `returnerer left dersom behandlingen er ikke under behandling`() {
            val søknadsbehandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            søknadsbehandling.validerKanOppdatere(ObjectMother.saksbehandler(), "en error msg").isLeft() shouldBe true
        }
    }
}
