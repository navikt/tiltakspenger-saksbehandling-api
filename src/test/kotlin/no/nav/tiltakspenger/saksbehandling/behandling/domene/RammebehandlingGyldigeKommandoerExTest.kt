package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.leggTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando
import org.junit.jupiter.api.Test

/**
 * Verifiserer at [finnGyldigeKommandoer] gir de samme svarene som `kanX`-predikatene domenet håndhever.
 */
class RammebehandlingGyldigeKommandoerExTest {
    private val saksbehandler = ObjectMother.saksbehandler()
    private val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999")
    private val beslutter = ObjectMother.beslutter()
    private val annenBeslutter = ObjectMother.beslutter(navIdent = "B99999")
    private val saksbehandlerOgBeslutter = ObjectMother.saksbehandlerOgBeslutter()
    private val utenRoller = ObjectMother.saksbehandlerUtenTilgang()

    private fun Rammebehandling.settPåVentAv(saksbehandler: Saksbehandler): Rammebehandling =
        settPåVent(
            kommando = SettRammebehandlingPåVentKommando(
                sakId = sakId,
                rammebehandlingId = id,
                oppdaterPåVent = false,
                begrunnelse = "venter på noe",
                saksbehandler = saksbehandler,
                frist = null,
            ),
            clock = ObjectMother.clock,
        ).getOrFail().first

    @Test
    fun `klar til behandling - saksbehandler kan tildele seg selv og avbryte`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .leggTilbakeRammebehandling(saksbehandler, ObjectMother.clock).getOrNull()!!.first

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelSaksbehandler,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `under behandling - tildelt saksbehandler kan legge tilbake, sette på vent og avbryte`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler,
            SaksbehandlerBehandlingKommando.SettPåVent,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `under behandling - en annen saksbehandler kan overta og avbryte`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(annenSaksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.OvertaSaksbehandler,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `under behandling - en beslutter kan bare avbryte`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    /**
     * I [Rammebehandlingsstatus.KLAR_TIL_BESLUTNING] er det saksbehandleren på behandlingen som kan avbryte.
     * Beslutteren har ikke tatt behandlingen enda, og får derfor bare tildelingen.
     */
    @Test
    fun `klar til beslutning - beslutter kan tildele seg selv, men ikke avbryte`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelBeslutter,
        )
    }

    @Test
    fun `klar til beslutning - saksbehandleren som sendte til beslutning kan ikke tildele seg selv, men kan avbryte`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.AngreSendTilBeslutning,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `under beslutning - tildelt beslutter kan legge tilbake, sette på vent og avbryte`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )

        behandling.status shouldBe Rammebehandlingsstatus.UNDER_BESLUTNING
        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.LeggTilbakeBeslutter,
            SaksbehandlerBehandlingKommando.SettPåVent,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `under beslutning - en annen beslutter kan overta, men ikke avbryte`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )

        behandling.finnGyldigeKommandoer(annenBeslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.OvertaBeslutter,
        )
    }

    /**
     * I [Rammebehandlingsstatus.UNDER_BESLUTNING] er det beslutteren på behandlingen som kan avbryte.
     * Saksbehandleren har gitt fra seg behandlingen, men kan angre sendingen til beslutning selv om en beslutter har tatt den.
     */
    @Test
    fun `under beslutning - saksbehandleren på behandlingen kan bare angre sendingen til beslutning`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )

        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.AngreSendTilBeslutning,
        )
    }

    @Test
    fun `vedtatt behandling har ingen gyldige kommandoer`() {
        val behandling = ObjectMother.nyVedtattSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.VEDTATT
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe emptyList()
        behandling.finnGyldigeKommandoer(beslutter) shouldBe emptyList()
    }

    @Test
    fun `revurdering følger de samme reglene som søknadsbehandling`() {
        val revurdering = ObjectMother.nyOpprettetRevurderingStans(saksbehandler = saksbehandler)

        revurdering.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        revurdering.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler,
            SaksbehandlerBehandlingKommando.SettPåVent,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
        revurdering.finnGyldigeKommandoer(annenSaksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.OvertaSaksbehandler,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `avbrutt søknadsbehandling kan kun gjenåpnes, og kun av en saksbehandler`() {
        val behandling = ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.AVBRUTT
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(SaksbehandlerBehandlingKommando.Gjenåpne)
        behandling.finnGyldigeKommandoer(beslutter) shouldBe emptyList()
    }

    @Test
    fun `bruker med både saksbehandler- og beslutterrolle kan overta fra saksbehandleren`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(saksbehandlerOgBeslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.OvertaSaksbehandler,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `bruker med både saksbehandler- og beslutterrolle kan tildele seg som beslutter`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(saksbehandlerOgBeslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelBeslutter,
        )
    }

    /**
     * Avbryting har ingen rollesjekk i `kanAvbryte` - den håndheves i `AvbrytSøknadOgBehandlingRoute`.
     * Kommandoen skal likevel ikke annonseres til leseroller eller brukere uten roller, så [finnGyldigeKommandoer] shortcircuiter dem til en tom liste.
     */
    @Test
    fun `bruker uten saksbehandler- eller beslutterrolle får ingen gyldige kommandoer`() {
        val underBehandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
        val klarTilBehandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .leggTilbakeRammebehandling(saksbehandler, ObjectMother.clock).getOrNull()!!.first

        klarTilBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        listOf(underBehandling, klarTilBehandling).forEach { behandling ->
            listOf(utenRoller, ObjectMother.veileder(), ObjectMother.utvikler()).forEach { bruker ->
                behandling.finnGyldigeKommandoer(bruker) shouldBe emptyList()
            }
        }
    }

    /**
     * En behandling på vent går tilbake til [Rammebehandlingsstatus.KLAR_TIL_BEHANDLING].
     * `kanTaBehandling` ser ikke på ventestatus, så tildeling annonseres ved siden av gjenopptaking.
     */
    @Test
    fun `behandling på vent tilbyr oppdatering og gjenoppta i stedet for sett på vent`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .settPåVentAv(saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        behandling.ventestatus.erSattPåVent shouldBe true
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelSaksbehandler,
            SaksbehandlerBehandlingKommando.OppdaterSettPåVent,
            SaksbehandlerBehandlingKommando.Gjenoppta,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `behandling på vent - en annen saksbehandler kan også oppdatere og gjenoppta`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .settPåVentAv(saksbehandler)

        behandling.finnGyldigeKommandoer(annenSaksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelSaksbehandler,
            SaksbehandlerBehandlingKommando.OppdaterSettPåVent,
            SaksbehandlerBehandlingKommando.Gjenoppta,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `behandling på vent - en beslutter kan ikke oppdatere eller gjenoppta en behandling som er klar til behandling`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .settPåVentAv(saksbehandler)

        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `behandling på vent under beslutning - beslutteren kan oppdatere og gjenoppta`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        ).settPåVentAv(beslutter)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelBeslutter,
            SaksbehandlerBehandlingKommando.OppdaterSettPåVent,
            SaksbehandlerBehandlingKommando.Gjenoppta,
        )
    }

    @Test
    fun `behandling på vent under beslutning tilbyr verken angring eller oppdatering til saksbehandleren`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        ).settPåVentAv(beslutter)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        val kommandoer = behandling.finnGyldigeKommandoer(saksbehandler)
        kommandoer.contains(SaksbehandlerBehandlingKommando.AngreSendTilBeslutning) shouldBe false
        kommandoer.contains(SaksbehandlerBehandlingKommando.OppdaterSettPåVent) shouldBe false
    }

    /**
     * `settPåVent`, oppdatering av ventestatus og `gjenoppta` er rollefrie for automatiske behandlinger fordi den automatiske saksbehandlingen selv bruker dem.
     * Testen dokumenterer at det også slår ut på kommandoene som annonseres til en vanlig bruker.
     */
    @Test
    fun `under automatisk behandling - sett på vent og avbryt annonseres`() {
        val behandling = ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()

        behandling.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.SettPåVent,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `under automatisk behandling på vent - en saksbehandler kan oppdatere og gjenoppta`() {
        val behandling = ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()
            .settPåVentAv(AUTOMATISK_SAKSBEHANDLER)

        behandling.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.OppdaterSettPåVent,
            SaksbehandlerBehandlingKommando.Gjenoppta,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    private fun automatiskBehandlingKlarTilBeslutning(): Søknadsbehandling =
        ObjectMother.nySøknadsbehandlingKlarTilBeslutning(
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            automatiskBehandling = true,
        )

    @Test
    fun `automatisk behandling klar til beslutning - saksbehandler kan hente tilbake`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.HentTilbake,
        )
    }

    @Test
    fun `automatisk behandling klar til beslutning - beslutter kan tildele seg, men ikke hente tilbake`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelBeslutter,
        )
    }

    @Test
    fun `automatisk behandling klar til beslutning - bruker med begge roller får både tildel beslutter og hent tilbake`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        behandling.finnGyldigeKommandoer(saksbehandlerOgBeslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelBeslutter,
            SaksbehandlerBehandlingKommando.HentTilbake,
        )
    }

    @Test
    fun `automatisk behandling på vent hos beslutter - hent tilbake annonseres ikke`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            beslutter = beslutter,
            automatiskBehandling = true,
        ).settPåVentAv(beslutter)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.ventestatus.erSattPåVent shouldBe true
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe emptyList()
    }

    @Test
    fun `manuell behandling klar til beslutning - hent tilbake annonseres ikke til en annen saksbehandler`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(annenSaksbehandler) shouldBe emptyList()
    }
}
