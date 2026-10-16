package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando
import org.junit.jupiter.api.Test

class MeldekortbehandlingGyldigeKommandoerExTest {

    private val saksbehandler = ObjectMother.saksbehandler()
    private val annenSaksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999")
    private val beslutter = ObjectMother.beslutter()

    private fun Meldekortbehandling.settPåVentAv(utøver: Saksbehandler): Meldekortbehandling = settPåVent(
        kommando = SettMeldekortbehandlingPåVentKommando(
            sakId = sakId,
            meldekortId = id,
            oppdaterPåVent = false,
            begrunnelse = "venter på noe",
            frist = null,
            saksbehandler = utøver,
            correlationId = CorrelationId.generate(),
        ),
        clock = TikkendeKlokke(),
    ).getOrFail()

    /**
     * Leseroller (veileder, utvikler) kan se behandlingen, men ikke utføre kommandoer på den.
     * De får derfor alltid en tom liste, uavhengig av behandlingens tilstand.
     */
    @Test
    fun `bruker uten saksbehandler- eller beslutterrolle får ingen gyldige kommandoer`() {
        val behandling = ObjectMother.meldekortUnderBehandling()

        listOf(ObjectMother.saksbehandlerUtenTilgang(), ObjectMother.veileder(), ObjectMother.utvikler()).forEach { bruker ->
            behandling.finnGyldigeKommandoer(bruker) shouldBe emptyList()
        }
    }

    @Test
    fun `meldekortbehandling som ikke er på vent tilbyr ikke oppdatering`() {
        val behandling = ObjectMother.meldekortUnderBehandling(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        )

        val kommandoer = behandling.finnGyldigeKommandoer(saksbehandler)
        kommandoer shouldContain SaksbehandlerBehandlingKommando.SettPåVent
        kommandoer shouldNotContain SaksbehandlerBehandlingKommando.OppdaterSettPåVent
    }

    @Test
    fun `klar til behandling på vent kan oppdateres av en hvilken som helst saksbehandler`() {
        val behandling = ObjectMother.meldekortUnderBehandling(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        ).settPåVentAv(saksbehandler)

        behandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
        listOf(saksbehandler, annenSaksbehandler).forEach { bruker ->
            val kommandoer = behandling.finnGyldigeKommandoer(bruker)
            kommandoer shouldContain SaksbehandlerBehandlingKommando.OppdaterSettPåVent
            kommandoer shouldNotContain SaksbehandlerBehandlingKommando.SettPåVent
        }
    }

    @Test
    fun `klar til behandling på vent kan ikke oppdateres av en beslutter`() {
        val behandling = ObjectMother.meldekortUnderBehandling(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        ).settPåVentAv(saksbehandler)

        behandling.finnGyldigeKommandoer(beslutter) shouldNotContain SaksbehandlerBehandlingKommando.OppdaterSettPåVent
    }

    @Test
    fun `klar til beslutning på vent kan oppdateres av en beslutter, men ikke av en saksbehandler`() {
        val behandling = ObjectMother.meldekortBehandletManuelt(
            status = MeldekortbehandlingStatus.UNDER_BESLUTNING,
            beslutter = beslutter.navIdent,
            iverksattTidspunkt = null,
        ).settPåVentAv(beslutter)

        behandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
        behandling.finnGyldigeKommandoer(beslutter) shouldContain SaksbehandlerBehandlingKommando.OppdaterSettPåVent
        behandling.finnGyldigeKommandoer(saksbehandler) shouldNotContain SaksbehandlerBehandlingKommando.OppdaterSettPåVent
    }
}
