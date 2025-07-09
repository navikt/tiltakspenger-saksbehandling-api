package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.junit.jupiter.api.Test

class OppdaterBarnetilleggRouteKtTest {
    @Test
    fun `kan oppdatere barnetillegg`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

                //language=json
                val innvilgelsesperiode =
                    """{"fraOgMed": "${behandling.søknad.vurderingsperiode().fraOgMed}", "tilOgMed": "${behandling.søknad.vurderingsperiode().tilOgMed}"}"""

                val (_, _, responseJson) = oppdaterBarnetillegg(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    //language=json
                    valgteTiltaksdeltakelser = """
                    [
                      {"eksternDeltagelseId": "${behandling.søknad.tiltak.id}","periode": $innvilgelsesperiode}
                    ]
                    """.trimIndent(),
                    innvilgelsesperiode = innvilgelsesperiode,
                    //language=json
                    barnetilleggDTO = """
                      {
                        "perioder": [
                          {"antallBarn": 1,"periode": $innvilgelsesperiode}
                         ],
                       "begrunnelse": "Dette er min begrunnelse for barnetillegg"
                     }
                    """.trimIndent(),
                )
            }
        }
    }
}
