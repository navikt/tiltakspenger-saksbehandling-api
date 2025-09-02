package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.enUkeEtterFixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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
            val overtaBehandling = behandling.overta(saksbehandler = nySaksbehandler, clock = enUkeEtterFixedClock)

            behandling.saksbehandler.shouldNotBe(nySaksbehandler.navIdent)
            overtaBehandling.getOrFail().saksbehandler shouldBe nySaksbehandler.navIdent
        }

        @Test
        fun `en beslutter kan overta behandlingen`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val nyBeslutter = ObjectMother.beslutter("nyNavIdent")
            val overtaBehandling = behandling.overta(saksbehandler = nyBeslutter, clock = enUkeEtterFixedClock)

            behandling.beslutter.shouldNotBe(nyBeslutter.navIdent)
            overtaBehandling.getOrFail().beslutter shouldBe nyBeslutter.navIdent
        }

        @Test
        fun `kan ikke overta dersom det er mindre enn 1 minutt siden noe er blitt gjort`() {
            val clock = Clock.fixed(Instant.parse("2025-07-01T12:29:01Z"), ZoneOffset.UTC)
            val behandling = ObjectMother.nyOpprettetSøknadsbehandling(clock = clock)
            val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "annenSaksbehandler")

            // 30 etter opprettelse av behandling
            val overtaClock = Clock.fixed(Instant.parse("2025-07-01T12:30:00Z"), ZoneOffset.UTC)

            behandling.overta(
                annenSaksbehandler,
                overtaClock,
            ) shouldBe KunneIkkeOvertaBehandling.BehandlingenErUnderAktivBehandling.left()
        }

        @Test
        fun `kan overta behandlingen dersom det er over 1 time siden noe er blitt gjort`() {
            val clock = Clock.fixed(Instant.parse("2025-07-01T12:00:00Z"), ZoneOffset.UTC)
            val behandling = ObjectMother.nyOpprettetSøknadsbehandling(clock = clock)
            val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "annenSaksbehandler")

            // 1:30 etter opprettelse av behandling
            val overtaClock = Clock.fixed(Instant.parse("2025-07-01T13:30:00Z"), ZoneOffset.UTC)

            behandling.overta(annenSaksbehandler, overtaClock).isRight() shouldBe true
        }
    }

    @Nested
    inner class ValiderKanOppdatereTest {
        @Test
        fun `returnerer left dersom utdøvende saksbehandler ikke eier behandlingen`() {
            val søknadsbehandling = ObjectMother.nyOpprettetSøknadsbehandling()
            val ikkeUtdøvendeSaksbehandler = ObjectMother.saksbehandler(navIdent = "ikkeUtdøvendeSaksbehandler")

            søknadsbehandling.validerKanOppdatere(ikkeUtdøvendeSaksbehandler).isLeft() shouldBe true
        }

        @Test
        fun `returnerer left dersom behandlingen er ikke under behandling`() {
            val søknadsbehandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            søknadsbehandling.validerKanOppdatere(ObjectMother.saksbehandler()).isLeft() shouldBe true
        }
    }

    @Nested
    inner class SettPåVent {
        val clock: Clock = Clock.fixed(Instant.parse("2025-08-05T12:30:00Z"), ZoneOffset.UTC)

        @Test
        fun `kan sette behandling på vent`() {
            val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

            val behandlingSattPåVent = behandling.settPåVent(beslutter, "Venter på mer informasjon", clock)

            behandlingSattPåVent.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
            behandlingSattPåVent.ventestatus.ventestatusHendelser.size shouldBe 1
            behandlingSattPåVent.ventestatus.ventestatusHendelser.last().let { it ->
                it.endretAv shouldBe beslutter.navIdent
                it.begrunnelse shouldBe "Venter på mer informasjon"
                it.erSattPåVent shouldBe true
            }
        }
    }

    @Nested
    inner class Gjenoppta {
        val clock: Clock = Clock.fixed(Instant.parse("2025-08-05T12:30:00Z"), ZoneOffset.UTC)

        @Test
        fun `kan gjenoppta behandling som er satt på vent`() {
            val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
            val behandling =
                ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

            val behandlingSattPåVent = behandling.settPåVent(beslutter, "Venter på mer informasjon", clock)
            val gjenopptattBehandling = behandlingSattPåVent.gjenoppta(beslutter, clock)

            gjenopptattBehandling.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
            gjenopptattBehandling.ventestatus.erSattPåVent shouldBe false
        }

        @Test
        fun `kan ikke gjenoppta behandling som ikke er satt på vent`() {
            val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
            val behandling =
                ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

            assertThrows<IllegalArgumentException> {
                behandling.gjenoppta(beslutter, clock)
            }
        }
    }
}
