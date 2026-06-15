package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.overta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgOverta
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutterOgOverta
import org.junit.jupiter.api.Test

class OvertaMeldekortbehandlingRouteTest {
    @Test
    fun `saksbehandler kan overta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling) = this.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgOverta(
                tac,
                overtarFraSaksbehandler = ObjectMother.saksbehandler("overtarFraSaksbehandler"),
                saksbehandlerSomOvertar = ObjectMother.saksbehandler("saksbehandlerSomOvertar"),
            )!!
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            meldekortbehandling.saksbehandler.shouldBe("saksbehandlerSomOvertar")
        }
    }

    @Test
    fun `beslutter kan overta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling) = this.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutterOgOverta(
                tac = tac,
                overtarFraBeslutter = ObjectMother.beslutter("overtarFraBeslutter"),
                beslutterSomOvertar = ObjectMother.beslutter("beslutterSomOvertar"),
            )!!
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
            meldekortbehandling.beslutter shouldBe "beslutterSomOvertar"
        }
    }
}
