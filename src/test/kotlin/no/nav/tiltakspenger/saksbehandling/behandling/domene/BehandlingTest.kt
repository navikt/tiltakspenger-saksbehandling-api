package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.enUkeEtterFixedClock
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
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
        fun `kan ikke overta dersom det er mindre enn 1 time siden noe er blitt gjort`() {
            val clock = Clock.fixed(Instant.parse("2025-07-01T12:00:00Z"), ZoneOffset.UTC)
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
        fun `kaster tilgangsexception dersom saksbehandler ikke er saksbehandler`() {
            assertThrows<TilgangException> {
                val saksbehandler = ObjectMother.saksbehandler(roller = Saksbehandlerroller(emptyList()))
                val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
                behandling.validerKanOppdatere(saksbehandler)
            }
        }

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
        @Test
        fun `kan sette behandling på vent`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val saksbehandler = ObjectMother.saksbehandler()
            val tidspunkt = (5.august(2025).atStartOfDay())

            val behandlingSattPåVent = behandling.settPåVent(saksbehandler, "Venter på mer informasjon", tidspunkt)

            behandlingSattPåVent.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
            behandlingSattPåVent.sattPåVent.erSattPåVent shouldBe true
            behandlingSattPåVent.sattPåVent.sattPåVentBegrunnelser.size shouldBe 1
            behandlingSattPåVent.sattPåVent.sattPåVentBegrunnelser.last().let { it ->
                it.sattPåVentAv shouldBe saksbehandler.navIdent
                it.begrunnelse shouldBe "Venter på mer informasjon"
                it.tidspunkt shouldBe tidspunkt
            }
        }
    }

    @Nested
    inner class Gjenoppta {
        @Test
        fun `kan gjenoppta behandling som er satt på vent`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val saksbehandler = ObjectMother.saksbehandler()
            val tidspunkt = (5.august(2025).atStartOfDay())

            val behandlingSattPåVent = behandling.settPåVent(saksbehandler, "Venter på mer informasjon", tidspunkt)
            val gjenopptattBehandling = behandlingSattPåVent.gjenoppta(saksbehandler, tidspunkt)

            gjenopptattBehandling.status shouldBe Behandlingsstatus.UNDER_BESLUTNING
            gjenopptattBehandling.sattPåVent.erSattPåVent shouldBe false
        }

        @Test
        fun `kan ikke gjenoppta behandling som ikke er satt på vent`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val saksbehandler = ObjectMother.saksbehandler()

            assertThrows<IllegalArgumentException> {
                behandling.gjenoppta(saksbehandler, LocalDateTime.now())
            }
        }
    }
}
