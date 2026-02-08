package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.enUkeEtterFixedClock
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RammebehandlingTest {
    private val correlationId = CorrelationId.generate()

    @Test
    fun `kan avbryte en behandling`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        val avbruttBehandling = behandling.avbryt(
            avbruttAv = ObjectMother.saksbehandler(),
            begrunnelse = "begrunnelse".toNonBlankString(),
            tidspunkt = førsteNovember24,
            skalAvbryteSøknad = true,
        )

        avbruttBehandling.erAvsluttet shouldBe true
        avbruttBehandling.avbrutt.let {
            it shouldNotBe null
            it!!.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            it.begrunnelse.value shouldBe "begrunnelse"
            it.tidspunkt shouldBe førsteNovember24
        }
        avbruttBehandling.søknad.avbrutt shouldNotBe null
        avbruttBehandling.avbrutt shouldBe avbruttBehandling.søknad.avbrutt
        avbruttBehandling.status shouldBe Rammebehandlingsstatus.AVBRUTT
    }

    @Test
    fun `kan avbryte en automatisk opprettet behandling`() {
        val behandling = ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()
        val avbruttBehandling = behandling.avbryt(
            avbruttAv = ObjectMother.saksbehandler(),
            begrunnelse = "begrunnelse".toNonBlankString(),
            tidspunkt = førsteNovember24,
            skalAvbryteSøknad = true,
        )

        avbruttBehandling.erAvsluttet shouldBe true
        avbruttBehandling.avbrutt.let {
            it shouldNotBe null
            it!!.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            it.begrunnelse.value shouldBe "begrunnelse"
            it.tidspunkt shouldBe førsteNovember24
        }
        avbruttBehandling.søknad.avbrutt shouldNotBe null
        avbruttBehandling.avbrutt shouldBe avbruttBehandling.søknad.avbrutt
        avbruttBehandling.status shouldBe Rammebehandlingsstatus.AVBRUTT
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
                begrunnelse = "begrunnelse".toNonBlankString(),
                tidspunkt = førsteNovember24,
                skalAvbryteSøknad = true,
            )
        }
    }

    @Nested
    inner class TaBehandling {
        val clock = fixedClock

        @Test
        fun `en saksbehandler kan ta en ikke tildelt behandling`() {
            val behandling = ObjectMother.nyAutomatiskSøknadsbehandlingManuellBehandling()
            val saksbehandler = ObjectMother.saksbehandler()

            val taBehandling = behandling.taBehandling(saksbehandler, clock)

            taBehandling.saksbehandler shouldBe saksbehandler.navIdent
            taBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        }

        @Test
        fun `en beslutter kan ta behandlingen`() {
            val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning()
            val beslutter = ObjectMother.beslutter()
            val taBehandling = behandling.taBehandling(beslutter, clock)

            taBehandling.beslutter shouldBe beslutter.navIdent
            taBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
        }
    }

    @Nested
    inner class Overta {
        @Test
        fun `en saksbehandler kan overta behandlingen`() {
            val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
            val nySaksbehandler = ObjectMother.saksbehandler("nyNavIdent")
            val overtaBehandling =
                behandling.overta(saksbehandler = nySaksbehandler, correlationId, clock = enUkeEtterFixedClock)

            behandling.saksbehandler.shouldNotBe(nySaksbehandler.navIdent)
            overtaBehandling.getOrFail().saksbehandler shouldBe nySaksbehandler.navIdent
        }

        @Test
        fun `en beslutter kan overta behandlingen`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning()
            val nyBeslutter = ObjectMother.beslutter("nyNavIdent")
            val overtaBehandling =
                behandling.overta(saksbehandler = nyBeslutter, correlationId, clock = enUkeEtterFixedClock)

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
                correlationId,
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

            behandling.overta(annenSaksbehandler, correlationId, overtaClock).isRight() shouldBe true
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
        fun `kaster exception dersom man prøver å sette behandling (klar til behandling) på vent`() {
            val saksbehandler = ObjectMother.saksbehandler()
            val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
                .leggTilbakeBehandling(saksbehandler = saksbehandler, clock = clock)

            assertThrows<IllegalStateException> {
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = behandling.sakId,
                    rammebehandlingId = behandling.id,
                    begrunnelse = "Denne kaster exception",
                    saksbehandler = saksbehandler,
                    venterTil = null,
                )
                behandling.settPåVent(kommando, clock)
            }
        }

        @Test
        fun `kan sette behandling (under behandling) på vent`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "Z111111")
            val behandling = ObjectMother.nySøknadsbehandlingUnderkjent(saksbehandler = saksbehandler)
            val kommando = SettRammebehandlingPåVentKommando(
                sakId = behandling.sakId,
                rammebehandlingId = behandling.id,
                begrunnelse = "Venter på mer informasjon",
                saksbehandler = saksbehandler,
                venterTil = null,
            )
            val behandlingSattPåVent = behandling.settPåVent(kommando, clock)

            behandlingSattPåVent.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            behandlingSattPåVent.saksbehandler shouldBe null
            behandlingSattPåVent.ventestatus.ventestatusHendelser.size shouldBe 1
            behandlingSattPåVent.ventestatus.ventestatusHendelser.last().let {
                it.endretAv shouldBe saksbehandler.navIdent
                it.begrunnelse shouldBe "Venter på mer informasjon"
                it.erSattPåVent shouldBe true
            }
        }

        @Test
        fun `kaster exception dersom man prøver å sette behandling (klar til beslutning) på vent`() {
            val saksbehandler = ObjectMother.saksbehandler()
            val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

            assertThrows<IllegalStateException> {
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = behandling.sakId,
                    rammebehandlingId = behandling.id,
                    begrunnelse = "Denne kaster exception",
                    saksbehandler = saksbehandler,
                    venterTil = null,
                )
                behandling.settPåVent(kommando, clock)
            }
        }

        @Test
        fun `kan sette behandling (under beslutning) på vent`() {
            val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

            val kommando = SettRammebehandlingPåVentKommando(
                sakId = behandling.sakId,
                rammebehandlingId = behandling.id,
                begrunnelse = "Venter på mer informasjon",
                saksbehandler = beslutter,
                venterTil = null,
            )
            val behandlingSattPåVent = behandling.settPåVent(kommando, clock)

            behandlingSattPåVent.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            behandlingSattPåVent.saksbehandler shouldBe behandling.saksbehandler
            behandlingSattPåVent.beslutter shouldBe null
            behandlingSattPåVent.ventestatus.ventestatusHendelser.size shouldBe 1
            behandlingSattPåVent.ventestatus.ventestatusHendelser.last().let {
                it.endretAv shouldBe beslutter.navIdent
                it.begrunnelse shouldBe "Venter på mer informasjon"
                it.erSattPåVent shouldBe true
            }
        }

        @Test
        fun `kaster exception dersom man prøver å sette behandling (vedtatt) på vent`() {
            val saksbehandler = ObjectMother.saksbehandler()
            val behandling = ObjectMother.nyVedtattSøknadsbehandling(saksbehandler = saksbehandler)

            assertThrows<IllegalStateException> {
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = behandling.sakId,
                    rammebehandlingId = behandling.id,
                    begrunnelse = "Denne kaster exception",
                    saksbehandler = saksbehandler,
                    venterTil = null,
                )
                behandling.settPåVent(kommando, clock)
            }
        }

        @Test
        fun `kaster exception dersom man prøver å sette behandling (avbrutt) på vent`() {
            val saksbehandler = ObjectMother.saksbehandler()
            val behandling = ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler)

            assertThrows<IllegalStateException> {
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = behandling.sakId,
                    rammebehandlingId = behandling.id,
                    begrunnelse = "Denne kaster exception",
                    saksbehandler = saksbehandler,
                    venterTil = null,
                )
                behandling.settPåVent(kommando, clock)
            }
        }
    }

    @Nested
    inner class Gjenoppta {
        val clock: Clock = Clock.fixed(Instant.parse("2025-08-05T12:30:00Z"), ZoneOffset.UTC)

        @Test
        fun `kan gjenoppta behandling (under behandling) som er satt på vent`() {
            val clock = TikkendeKlokke()
            runTest {
                val saksbehandler = ObjectMother.saksbehandler()
                val saksbehandler2 = ObjectMother.saksbehandler(navIdent = "saksbehandler2")
                val behandlingSattPåVent =
                    ObjectMother.nySøknadsbehandlingUnderkjent(saksbehandler = saksbehandler).let {
                        it.settPåVent(
                            SettRammebehandlingPåVentKommando(
                                sakId = it.sakId,
                                rammebehandlingId = it.id,
                                begrunnelse = "1",
                                saksbehandler = saksbehandler,
                                venterTil = null,
                            ),
                            clock,
                        )
                    }

                behandlingSattPåVent.saksbehandler shouldBe null
                behandlingSattPåVent.beslutter shouldBe null
                behandlingSattPåVent.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING

                val gjenopptattBehandling = behandlingSattPåVent.gjenoppta(
                    saksbehandler2,
                    correlationId,
                    clock,
                ) { behandlingSattPåVent.saksopplysninger }.getOrFail()

                gjenopptattBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                gjenopptattBehandling.saksbehandler shouldBe saksbehandler2.navIdent
                gjenopptattBehandling.ventestatus.erSattPåVent shouldBe false
                gjenopptattBehandling.beslutter shouldBe null
            }
        }

        @Test
        fun `kan gjenoppta behandling (under beslutning) som er satt på vent`() {
            val clock = TikkendeKlokke()
            runTest {
                val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
                val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = behandling.sakId,
                    rammebehandlingId = behandling.id,
                    begrunnelse = "Venter på mer informasjon",
                    saksbehandler = beslutter,
                    venterTil = null,
                )
                val behandlingSattPåVent = behandling.settPåVent(kommando, clock)

                behandlingSattPåVent.saksbehandler shouldBe behandling.saksbehandler
                behandlingSattPåVent.beslutter shouldBe null
                behandlingSattPåVent.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING

                val gjenopptattBehandling =
                    behandlingSattPåVent.gjenoppta(beslutter, correlationId, clock) { behandling.saksopplysninger }
                        .getOrFail()

                gjenopptattBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
                gjenopptattBehandling.ventestatus.erSattPåVent shouldBe false
                gjenopptattBehandling.saksbehandler shouldBe behandling.saksbehandler
                gjenopptattBehandling.beslutter shouldBe beslutter.navIdent
            }
        }

        @Test
        fun `saksbehandler kan gjenoppta en automatisk behandling som er satt på vent`() {
            runTest {
                val saksbehandler = ObjectMother.saksbehandler(navIdent = "Z111111")
                val clockPaVent = Clock.fixed(Instant.parse("2025-07-01T12:00:00Z"), ZoneOffset.UTC)
                val behandling = ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()
                val kommando = SettRammebehandlingPåVentKommando(
                    sakId = behandling.sakId,
                    rammebehandlingId = behandling.id,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    saksbehandler = AUTOMATISK_SAKSBEHANDLER,
                    venterTil = nå(clock).plusWeeks(1),
                )
                val behandlingSattPåVent = behandling.settPåVent(kommando, clockPaVent)
                val gjenopptaClock = Clock.fixed(Instant.parse("2025-07-01T13:30:00Z"), ZoneOffset.UTC)

                behandlingSattPåVent.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER.navIdent
                behandlingSattPåVent.beslutter shouldBe null
                behandlingSattPåVent.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                behandlingSattPåVent.ventestatus.erSattPåVent shouldBe true

                val gjenopptattBehandling = behandlingSattPåVent.gjenoppta(
                    saksbehandler,
                    correlationId,
                    gjenopptaClock,
                ) { behandling.saksopplysninger }.getOrFail()

                gjenopptattBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                gjenopptattBehandling.ventestatus.erSattPåVent shouldBe false
                gjenopptattBehandling.saksbehandler shouldBe saksbehandler.navIdent
                gjenopptattBehandling.beslutter shouldBe null
            }
        }

        @Test
        fun `kaster exception dersom man prøver å gjenoppta behandling (vedtatt)`() {
            runTest {
                val saksbehandler = ObjectMother.saksbehandler()
                val behandling = ObjectMother.nyVedtattSøknadsbehandling(saksbehandler = saksbehandler)

                assertThrows<IllegalStateException> {
                    val kommando = SettRammebehandlingPåVentKommando(
                        sakId = behandling.sakId,
                        rammebehandlingId = behandling.id,
                        begrunnelse = "Denne kaster exception og skal ikke kunne bli gjenopptatt",
                        saksbehandler = saksbehandler,
                        venterTil = null,
                    )
                    val behandlingPåVent = behandling.settPåVent(kommando, clock)
                    behandlingPåVent.gjenoppta(saksbehandler, correlationId, clock) { behandling.saksopplysninger }
                }
            }
        }

        @Test
        fun `kaster exception dersom man prøver å gjenoppta behandling (avbrutt)`() {
            runTest {
                val saksbehandler = ObjectMother.saksbehandler()
                val behandling = ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler)

                assertThrows<IllegalStateException> {
                    val kommando = SettRammebehandlingPåVentKommando(
                        sakId = behandling.sakId,
                        rammebehandlingId = behandling.id,
                        begrunnelse = "Denne kaster exception og skal ikke kunne bli gjenopptatt",
                        saksbehandler = saksbehandler,
                        venterTil = null,
                    )
                    val behandlingPåVent = behandling.settPåVent(kommando, clock)
                    behandlingPåVent.gjenoppta(saksbehandler, correlationId, clock) { behandling.saksopplysninger }
                }
            }
        }

        @Test
        fun `kan ikke gjenoppta behandling som ikke er satt på vent`() {
            runTest {
                val beslutter = ObjectMother.beslutter(navIdent = "Z111111")
                val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(beslutter = beslutter)

                assertThrows<IllegalArgumentException> {
                    behandling.gjenoppta(beslutter, correlationId, clock) { behandling.saksopplysninger }
                }
            }
        }
    }

    @Nested
    inner class Underkjenning {
        val attestering = Attestering(
            status = Attesteringsstatus.SENDT_TILBAKE,
            begrunnelse = "Manglende dokumentasjon".toNonBlankString(),
            beslutter = ObjectMother.beslutter().navIdent,
            tidspunkt = førsteNovember24,
        )

        @Test
        fun `underkjenner en behandling`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(automatiskBehandling = false)
            behandling.underkjenn(
                utøvendeBeslutter = ObjectMother.beslutter(),
                attestering = attestering,
                clock = fixedClock,
            ).let {
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING

                it.attesteringer.size shouldBe 1
                it.attesteringer.first().let { attestering ->
                    attestering.status shouldBe Attesteringsstatus.SENDT_TILBAKE
                    attestering.begrunnelse shouldBe "Manglende dokumentasjon".toNonBlankString()
                    attestering.beslutter shouldBe ObjectMother.beslutter().navIdent
                    attestering.tidspunkt shouldBe førsteNovember24
                }
            }
        }

        @Test
        fun `resetter ikke resultatet av en manuell søknadsbehandling når man underkjenner`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(automatiskBehandling = false)
            behandling.underkjenn(
                utøvendeBeslutter = ObjectMother.beslutter(),
                attestering = attestering,
                clock = fixedClock,
            ).let {
                it.resultat shouldBe behandling.resultat
            }
        }

        @Test
        fun `resetter resultatet av en automatisk søknadsbehandling når man underkjenner`() {
            val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(automatiskBehandling = true)
            behandling.underkjenn(
                utøvendeBeslutter = ObjectMother.beslutter(),
                attestering = attestering,
                clock = fixedClock,
            ).let {
                it.resultat shouldBe null
            }
        }
    }
}
