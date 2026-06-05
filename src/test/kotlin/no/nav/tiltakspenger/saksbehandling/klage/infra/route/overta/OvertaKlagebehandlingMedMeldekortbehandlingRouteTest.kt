package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtattMeldekortbehandlingMedKlageFraKlageRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtattMeldekortbehandlingMedKlageFraMeldekortRoute
import org.junit.jupiter.api.Test

class OvertaKlagebehandlingMedMeldekortbehandlingRouteTest {
    @Test
    fun `kan overta klagebehandling med meldekortbehandling fra klage`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val saksbehandlerSomOvertar = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling")
            val (_, oppdatertMeldekortbehandling, _) = overtattMeldekortbehandlingMedKlageFraKlageRoute(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
                saksbehandlerSomOvertarKlagebehandling = saksbehandlerSomOvertar,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent
            oppdatertKlagebehandling.behandlingId shouldBe listOf(oppdatertMeldekortbehandling.id)
            oppdatertKlagebehandling.åpenBehandlingId shouldBe oppdatertMeldekortbehandling.id

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent
            oppdatertMeldekortbehandling.klagebehandling.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent
        }
    }

    @Test
    fun `kan overta klagebehandling med meldekortbehandling fra meldekort`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val saksbehandlerSomOvertar = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling")
            val (_, oppdatertMeldekortbehandling, _) = overtattMeldekortbehandlingMedKlageFraMeldekortRoute(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
                saksbehandlerSomOvertarKlagebehandling = saksbehandlerSomOvertar,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent
            oppdatertKlagebehandling.behandlingId shouldBe listOf(oppdatertMeldekortbehandling.id)
            oppdatertKlagebehandling.åpenBehandlingId shouldBe oppdatertMeldekortbehandling.id

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent
            oppdatertMeldekortbehandling.klagebehandling.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent
        }
    }
}
