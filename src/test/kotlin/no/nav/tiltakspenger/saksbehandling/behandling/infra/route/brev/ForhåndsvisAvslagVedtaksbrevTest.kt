package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.junit.jupiter.api.Test

internal class ForhåndsvisAvslagVedtaksbrevTest {
    @Test
    fun `kan forhåndsvise vedtaksbrev for avslått søknadsbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
                val behandlingId = behandling.id
                val fritekstTilVedtaksbrev = "some_tekst"
                val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandlingId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    virkningsperiode = 1.januar(2025) til 31.mars(2025),
                    stansDato = null,
                    valgteHjemler = null,
                    barnetillegg = null,
                    resultat = BehandlingResultatDTO.AVSLAG,
                    avslagsgrunner = listOf(ValgtHjemmelForAvslagDTO.FremmetForSent),
                )
                responseJson shouldBe "pdf"
            }
        }
    }
}
