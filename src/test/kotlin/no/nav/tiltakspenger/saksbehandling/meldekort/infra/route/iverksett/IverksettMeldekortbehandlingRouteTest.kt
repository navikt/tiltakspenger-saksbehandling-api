package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.iverksett

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import org.junit.jupiter.api.Test

class IverksettMeldekortbehandlingRouteTest {
    @Test
    fun `saksbehandler kan iverksette meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgMeldekortbehandling(
                tac = tac,
            )!!
            meldekortbehandling.status shouldBe MeldekortBehandlingStatus.GODKJENT
        }
    }
}
