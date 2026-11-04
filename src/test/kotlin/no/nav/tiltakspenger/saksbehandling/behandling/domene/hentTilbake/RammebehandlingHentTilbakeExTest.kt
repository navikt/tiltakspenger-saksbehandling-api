package no.nav.tiltakspenger.saksbehandling.behandling.domene.hentTilbake

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.leggTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererSaksstatistikk
import org.junit.jupiter.api.Test

class RammebehandlingHentTilbakeExTest {
    private val saksbehandler = ObjectMother.saksbehandler()
    private val beslutter = ObjectMother.beslutter()
    private val saksbehandlerOgBeslutter = ObjectMother.saksbehandlerOgBeslutter()
    private val clock = ObjectMother.clock

    private fun automatiskBehandlingKlarTilBeslutning(): Søknadsbehandling =
        ObjectMother.nySøknadsbehandlingKlarTilBeslutning(
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            sendtTilBeslutning = nå(clock),
        )

    private fun revurderingKlarTilBeslutning(saksbehandler: Saksbehandler): Revurdering {
        val vedtaksperiode = ObjectMother.revurderingVedtaksperiode()
        return ObjectMother.nyRevurderingStansKlarTilBeslutning(
            saksbehandler = saksbehandler,
            stansFraOgMed = vedtaksperiode.fraOgMed,
            førsteDagSomGirRett = vedtaksperiode.fraOgMed,
            sisteDagSomGirRett = vedtaksperiode.tilOgMed,
        )
    }

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
            clock = clock,
        ).getOrFail().first

    @Test
    fun `kanHenteTilbake gir right for saksbehandler på automatisk behandling klar til beslutning`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
        behandling.kanHenteTilbake(saksbehandler) shouldBe Unit.right()
    }

    @Test
    fun `kanHenteTilbake gir right for bruker med både saksbehandler- og beslutterrolle`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        behandling.kanHenteTilbake(saksbehandlerOgBeslutter) shouldBe Unit.right()
    }

    @Test
    fun `kanHenteTilbake gir MåVæreSaksbehandler for ren beslutter`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        behandling.kanHenteTilbake(beslutter) shouldBe KanIkkeHenteTilbakeRammebehandling.MåVæreSaksbehandler.left()
    }

    @Test
    fun `kanHenteTilbake gir right for automatisk behandlet revurdering klar til beslutning`() {
        val revurdering = revurderingKlarTilBeslutning(AUTOMATISK_SAKSBEHANDLER)

        revurdering.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        revurdering.automatiskSaksbehandlet shouldBe true
        revurdering.kanHenteTilbake(saksbehandler) shouldBe Unit.right()
    }

    @Test
    fun `kanHenteTilbake gir ErIkkeAutomatiskBehandlet for manuelt behandlet revurdering`() {
        val revurdering = revurderingKlarTilBeslutning(saksbehandler)

        revurdering.kanHenteTilbake(saksbehandler) shouldBe KanIkkeHenteTilbakeRammebehandling.ErIkkeAutomatiskBehandlet.left()
    }

    @Test
    fun `kanHenteTilbake gir ErIkkeAutomatiskBehandlet for manuelt behandlet søknadsbehandling`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.kanHenteTilbake(saksbehandler) shouldBe KanIkkeHenteTilbakeRammebehandling.ErIkkeAutomatiskBehandlet.left()
        behandling.kanHenteTilbake(ObjectMother.saksbehandler(navIdent = "Z999999")) shouldBe
            KanIkkeHenteTilbakeRammebehandling.ErIkkeAutomatiskBehandlet.left()
    }

    @Test
    fun `kanHenteTilbake gir BehandlingenErSattPåVent når beslutter har satt behandlingen på vent`() {
        val behandling = ObjectMother.nySøknadsbehandlingUnderBeslutning(
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            beslutter = beslutter,
        ).settPåVentAv(beslutter)

        behandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandling.ventestatus.erSattPåVent shouldBe true
        behandling.kanHenteTilbake(saksbehandler) shouldBe KanIkkeHenteTilbakeRammebehandling.BehandlingenErSattPåVent.left()
    }

    @Test
    fun `kanHenteTilbake gir UgyldigStatus for alle andre statuser enn klar til beslutning`() {
        val behandlinger = listOf(
            ObjectMother.nyOpprettetAutomatiskSøknadsbehandling(),
            ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler)
                .leggTilbakeRammebehandling(saksbehandler, clock).getOrNull()!!.first,
            ObjectMother.nyOpprettetSøknadsbehandling(saksbehandler = saksbehandler),
            ObjectMother.nySøknadsbehandlingUnderBeslutning(saksbehandler = saksbehandler, beslutter = beslutter),
            ObjectMother.nyVedtattSøknadsbehandling(saksbehandler = saksbehandler),
            ObjectMother.nyAvbruttSøknadsbehandling(saksbehandler = saksbehandler),
        )

        behandlinger.map { it.status }.toSet() shouldBe Rammebehandlingsstatus.entries.toSet() - Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        behandlinger.forEach { behandling ->
            withClue(behandling.status) {
                behandling.kanHenteTilbake(saksbehandler) shouldBe
                    KanIkkeHenteTilbakeRammebehandling.UgyldigStatus(behandling.status).left()
            }
        }
    }

    @Test
    fun `hentTilbakeRammebehandling flytter behandlingen til saksbehandleren og nullstiller automatisk resultat`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        val (oppdatert, statistikkhendelser) = behandling.hentTilbakeRammebehandling(saksbehandler, fixedClock).getOrFail()

        oppdatert.shouldBeInstanceOf<Søknadsbehandling>() shouldBe behandling.copy(
            status = Rammebehandlingsstatus.UNDER_BEHANDLING,
            saksbehandler = saksbehandler.navIdent,
            sistEndret = nå(fixedClock),
            sendtTilBeslutning = null,
            resultat = null,
        )
        statistikkhendelser.hendelser.single().shouldBeInstanceOf<GenererSaksstatistikk>()
    }

    @Test
    fun `hentTilbakeRammebehandling flytter revurderingen til saksbehandleren og beholder resultatet`() {
        val revurdering = revurderingKlarTilBeslutning(AUTOMATISK_SAKSBEHANDLER)

        val (oppdatert, statistikkhendelser) = revurdering.hentTilbakeRammebehandling(saksbehandler, fixedClock).getOrFail()

        oppdatert.shouldBeInstanceOf<Revurdering>() shouldBe revurdering.copy(
            status = Rammebehandlingsstatus.UNDER_BEHANDLING,
            saksbehandler = saksbehandler.navIdent,
            sistEndret = nå(fixedClock),
            sendtTilBeslutning = null,
        )
        oppdatert.automatiskSaksbehandlet shouldBe false
        statistikkhendelser.hendelser.single().shouldBeInstanceOf<GenererSaksstatistikk>()
    }

    @Test
    fun `hentTilbakeRammebehandling kaster TilgangException for bruker uten saksbehandlerrolle`() {
        val behandling = automatiskBehandlingKlarTilBeslutning()

        shouldThrow<TilgangException> {
            behandling.hentTilbakeRammebehandling(beslutter, clock)
        }
    }

    @Test
    fun `hentTilbakeRammebehandling returnerer feilen fra kanHenteTilbake som left`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning(saksbehandler = saksbehandler)

        behandling.hentTilbakeRammebehandling(saksbehandler, clock) shouldBe
            KanIkkeHenteTilbakeRammebehandling.ErIkkeAutomatiskBehandlet.left()
    }
}
