package no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.lagtTilbakeMeldekortbehandlingMedKlageFraKlageRoute
import org.junit.jupiter.api.Test

class LeggTilbakeKlagebehandlingMedMeldekortbehandlingRouteTest {
    @Test
    fun `kan legge tilbake klagebehandling med meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, meldekortbehandlingLagtTilbake, _) = lagtTilbakeMeldekortbehandlingMedKlageFraKlageRoute(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val klagebehandlingLagtTilbake = requireNotNull(meldekortbehandlingLagtTilbake.klagebehandling)

            klagebehandlingLagtTilbake.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            klagebehandlingLagtTilbake.saksbehandler shouldBe null
            meldekortbehandlingLagtTilbake.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            meldekortbehandlingLagtTilbake.saksbehandler shouldBe null
            klagebehandlingLagtTilbake.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            klagebehandlingLagtTilbake.saksbehandler shouldBe null
        }
    }
}
