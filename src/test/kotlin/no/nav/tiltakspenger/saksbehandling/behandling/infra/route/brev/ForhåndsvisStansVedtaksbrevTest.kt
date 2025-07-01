package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import org.junit.jupiter.api.Test

internal class ForhåndsvisStansVedtaksbrevTest {
    @Test
    fun `kan forhåndsvise vedtaksbrev for innvilget søknadsbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurderingStans(
                    tac = tac,
                    virkingsperiode = 1.januar(2025) til 31.mars(2025),
                )
                val behandlingId = revurdering.id
                val fritekstTilVedtaksbrev = "some_tekst"
                val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandlingId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    virkningsperiode = null,
                    stansDato = 1.februar(2025),
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.LønnFraAndre),
                    barnetillegg = null,
                    resultat = BehandlingResultatDTO.STANS,
                    avslagsgrunner = null,
                )
                responseJson shouldBe "pdf"
            }
        }
    }
}
