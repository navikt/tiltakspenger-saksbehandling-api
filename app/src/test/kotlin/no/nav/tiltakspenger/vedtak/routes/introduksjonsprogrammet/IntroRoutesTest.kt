package no.nav.tiltakspenger.vedtak.routes.introduksjonsprogrammet

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingUavklart
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.introduksjonsprogrammet.IntroVilkårDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.DeltagelseDTO.DELTAR_IKKE
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.routes
import org.junit.jupiter.api.Test

class IntroRoutesTest {

    @Test
    fun `test at endepunkt for henting og lagring av intro fungerer`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = this.førstegangsbehandlingUavklart()
            val behandlingId = sak.førstegangsbehandling!!.id
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/$behandlingId/vilkar/introduksjonsprogrammet")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val introVilkår = objectMapper.readValue<IntroVilkårDTO>(bodyAsText())
                    introVilkår.avklartSaksopplysning.periodeMedDeltagelse.deltagelse shouldBe DELTAR_IKKE
                }
            }
        }
    }

    @Test
    fun `test at søknaden blir gjenspeilet i introduksjonsprogrammet vilkåret`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = this.førstegangsbehandlingUavklart(
                deltarPåIntroduksjonsprogram = false,
            )
            val behandlingId = sak.førstegangsbehandling!!.id
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/$behandlingId/vilkar/introduksjonsprogrammet")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val introVilkår = objectMapper.readValue<IntroVilkårDTO>(bodyAsText())
                    introVilkår.avklartSaksopplysning.periodeMedDeltagelse.deltagelse shouldBe DELTAR_IKKE
                }
            }
        }
    }
}
