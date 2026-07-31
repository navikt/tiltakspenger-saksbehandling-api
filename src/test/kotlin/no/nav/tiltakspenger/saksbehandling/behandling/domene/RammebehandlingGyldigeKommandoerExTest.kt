package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.leggTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando
import org.junit.jupiter.api.Test

/**
 * Verifiserer at [finnGyldigeKommandoer] gir de samme svarene som `kanX`-predikatene domenet håndhever.
 */
internal class RammebehandlingGyldigeKommandoerExTest {
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
                begrunnelse = "venter på noe",
                saksbehandler = saksbehandler,
                frist = null,
            ),
            clock = ObjectMother.clock,
        ).first

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

    @Test
    fun `klar til beslutning - beslutter kan tildele seg selv og avbryte`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelBeslutter,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `klar til beslutning - saksbehandleren som sendte til beslutning kan ikke tildele seg selv`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
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
    fun `under beslutning - en annen beslutter kan overta og avbryte`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )

        behandling.finnGyldigeKommandoer(annenBeslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.OvertaBeslutter,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `under beslutning - saksbehandleren på behandlingen kan verken overta eller legge tilbake`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        )

        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.Avbryt,
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
    fun `avbrutt behandling har ingen gyldige kommandoer`() {
        val behandling = ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.AVBRUTT
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe emptyList()
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
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    /**
     * Avbryting har ingen rollesjekk i domenet - den håndheves i `AvbrytSøknadOgBehandlingRoute`.
     * Testen dokumenterer dagens oppførsel: kommandoen annonseres også til en bruker uten roller.
     */
    @Test
    fun `bruker uten roller får kun avbryt`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)

        behandling.finnGyldigeKommandoer(utenRoller) shouldBe listOf(
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    /**
     * En behandling på vent går tilbake til [Rammebehandlingsstatus.KLAR_TIL_BEHANDLING].
     * `kanTaBehandling` ser ikke på ventestatus, så tildeling annonseres ved siden av gjenopptaking.
     */
    @Test
    fun `behandling på vent tilbyr gjenoppta i stedet for sett på vent`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .settPåVentAv(saksbehandler)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        behandling.ventestatus.erSattPåVent shouldBe true
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelSaksbehandler,
            SaksbehandlerBehandlingKommando.Gjenoppta,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `behandling på vent - en annen saksbehandler kan også gjenoppta`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .settPåVentAv(saksbehandler)

        behandling.finnGyldigeKommandoer(annenSaksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelSaksbehandler,
            SaksbehandlerBehandlingKommando.Gjenoppta,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `behandling på vent - en beslutter kan ikke gjenoppta en behandling som er klar til behandling`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
            .settPåVentAv(saksbehandler)

        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    @Test
    fun `behandling på vent under beslutning - beslutteren kan gjenoppta`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = saksbehandler,
            beslutter = beslutter,
        ).settPåVentAv(beslutter)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.finnGyldigeKommandoer(beslutter) shouldBe listOf(
            SaksbehandlerBehandlingKommando.TildelBeslutter,
            SaksbehandlerBehandlingKommando.Gjenoppta,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }

    /**
     * `settPåVent` og `gjenoppta` er rollefrie for automatiske behandlinger fordi den automatiske saksbehandlingen selv bruker dem.
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
    fun `under automatisk behandling på vent - en saksbehandler kan gjenoppta`() {
        val behandling = ObjectMother.nyOpprettetAutomatiskSøknadsbehandling()
            .settPåVentAv(AUTOMATISK_SAKSBEHANDLER)

        behandling.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        behandling.finnGyldigeKommandoer(saksbehandler) shouldBe listOf(
            SaksbehandlerBehandlingKommando.Gjenoppta,
            SaksbehandlerBehandlingKommando.Avbryt,
        )
    }
}
