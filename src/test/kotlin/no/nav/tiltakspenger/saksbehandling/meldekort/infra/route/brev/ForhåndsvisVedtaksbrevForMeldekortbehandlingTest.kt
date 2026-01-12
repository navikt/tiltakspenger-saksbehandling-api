package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.brev

import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgForhåndsvisVedtaksbrevForMeldekortbehandling
import org.junit.jupiter.api.Test

class ForhåndsvisVedtaksbrevForMeldekortbehandlingTest {
    @Test
    fun `kan forhåndsvise vedtaksbrev`() {
        withTestApplicationContext { tac ->
            val (_, _, _, _, _) = iverksettSøknadsbehandlingOgForhåndsvisVedtaksbrevForMeldekortbehandling(
                tac = tac,
            )!!
        }
    }
}
