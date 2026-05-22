package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgTaKlagebehandlingMedMeldekortbehandling
import org.junit.jupiter.api.Test

class TaKlagebehandlingMedMeldekortbehandlingRouteTest {
    @Test
    fun `kan ta klagebehandling med meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val saksbehandlerSomTar = ObjectMother.saksbehandler("saksbehandlerSomTarKlagebehandling")
            val (_, meldekortbehandlingTatt, _) = iverksettSøknadsbehandlingOgTaKlagebehandlingMedMeldekortbehandling(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
                saksbehandlerSomTarKlagebehandling = saksbehandlerSomTar,
            )!!
            val klagebehandlingTatt = requireNotNull(meldekortbehandlingTatt.klagebehandling)

            klagebehandlingTatt.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            klagebehandlingTatt.saksbehandler shouldBe saksbehandlerSomTar.navIdent
            meldekortbehandlingTatt.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            meldekortbehandlingTatt.saksbehandler shouldBe saksbehandlerSomTar.navIdent
            klagebehandlingTatt.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            klagebehandlingTatt.saksbehandler shouldBe saksbehandlerSomTar.navIdent
        }
    }
}
