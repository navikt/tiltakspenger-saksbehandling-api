package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.startRevurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
import org.junit.jupiter.api.Test

class SendRevurderingTilBeslutningTest {
    @Test
    fun `kan sende revurdering til beslutning`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurdering(tac)
                taBehanding(tac, revurdering.id)

                val stansdato = sak.vedtaksliste.førsteDagSomGirRett!!.plusDays(1)

                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/sak/${sak.id}/revurdering/${revurdering.id}/sendtilbeslutning")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                        saksbehandler = ObjectMother.saksbehandler(),
                    ),
                ) {
                    setBody(
                        """
                            {
                                "begrunnelse": "Begrunnelse",
                                "stansDato": "$stansdato"
                            }
                        """.trimIndent(),
                    )
                }.apply {
                    val bodyAsText = this.bodyAsText()
                    withClue(
                        "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
                    ) {
                        status shouldBe HttpStatusCode.OK
                        val oppdatertRevurdering = objectMapper.readValue<BehandlingDTO>(bodyAsText)
                        oppdatertRevurdering.status shouldBe BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
                    }
                }
            }
        }
    }

    @Test
    fun `send revurdering til beslutning feiler hvis stansdato er før innvilgelsesperioden`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurdering(tac)
                taBehanding(tac, revurdering.id)

                val stansdato = sak.vedtaksliste.førsteDagSomGirRett!!.minusDays(2)

                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/sak/${sak.id}/revurdering/${revurdering.id}/sendtilbeslutning")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                        saksbehandler = ObjectMother.saksbehandler(),
                    ),
                ) {
                    setBody(
                        """
                            {
                                "begrunnelse": "Begrunnelse",
                                "stansDato": "$stansdato"
                            }
                        """.trimIndent(),
                    )
                }.apply {
                    val bodyAsText = this.bodyAsText()
                    withClue(
                        "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
                    ) {
                        status shouldBe HttpStatusCode.InternalServerError
                    }
                }
            }
        }
    }

    @Test
    fun `send revurdering til beslutning feiler hvis stansdato er etter innvilgelsesperioden`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurdering(tac)
                taBehanding(tac, revurdering.id)

                val stansdato = sak.sisteDagSomGirRett!!.plusDays(2)

                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/sak/${sak.id}/revurdering/${revurdering.id}/sendtilbeslutning")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                        saksbehandler = ObjectMother.saksbehandler(),
                    ),
                ) {
                    setBody(
                        """
                            {
                                "begrunnelse": "Begrunnelse",
                                "stansDato": "$stansdato"
                            }
                        """.trimIndent(),
                    )
                }.apply {
                    val bodyAsText = this.bodyAsText()
                    withClue(
                        "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
                    ) {
                        status shouldBe HttpStatusCode.InternalServerError
                    }
                }
            }
        }
    }
}
