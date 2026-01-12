package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.ta

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehadlingLeggTilbakeOgTaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.`iverksettSøknadsbehandlingOgBeslutterTarBehandling`
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import org.junit.jupiter.api.Test

class TaMeldekortbehandlingRouteTest {
    @Test
    fun `beslutter kan ta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgBeslutterTarBehandling(tac)!!
            meldekortbehandling.status shouldBe MeldekortBehandlingStatus.UNDER_BESLUTNING
        }
    }

    @Test
    fun `saksbehandler kan ta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehadlingLeggTilbakeOgTaMeldekortbehandling(tac)!!
            meldekortbehandling.status shouldBe MeldekortBehandlingStatus.UNDER_BEHANDLING
        }
    }
}
