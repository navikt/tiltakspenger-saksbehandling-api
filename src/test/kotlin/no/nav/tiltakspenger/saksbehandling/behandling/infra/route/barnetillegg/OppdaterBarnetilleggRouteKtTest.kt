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
                val tiltaksdeltagelseperiodeDetErSøktOm =
                    """{"fraOgMed": "${behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm().fraOgMed}", "tilOgMed": "${behandling.søknad.tiltaksdeltagelseperiodeDetErSøktOm().tilOgMed}"}"""

                // vi har en sjekk på funksjonen for at barnetillegg er satt i json
                oppdaterBarnetillegg(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    //language=json
                    valgteTiltaksdeltakelser = """
                    [
                      {"eksternDeltagelseId": "${behandling.søknad.tiltak.id}","periode": $tiltaksdeltagelseperiodeDetErSøktOm}
                    ]
                    """.trimIndent(),
                    innvilgelsesperiode = tiltaksdeltagelseperiodeDetErSøktOm,
                    //language=json
                    barnetilleggDTO = """
                      {
                        "perioder": [
                          {"antallBarn": 1,"periode": $tiltaksdeltagelseperiodeDetErSøktOm}
                         ],
                       "begrunnelse": "Dette er min begrunnelse for barnetillegg"
                     }
                    """.trimIndent(),
                )
            }
        }
    }
}
