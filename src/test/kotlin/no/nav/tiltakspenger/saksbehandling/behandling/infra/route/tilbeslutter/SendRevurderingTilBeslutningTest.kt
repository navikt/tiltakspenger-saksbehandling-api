package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingResultatDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class SendRevurderingTilBeslutningTest {
    @Test
    fun `kan sende revurdering stans til beslutning`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurderingStans(tac)

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
                        @Language("JSON")
                        """
                            {
                                "type": "STANS",
                                "begrunnelse": "Begrunnelse",
                                "stans": {
                                    "stansFraOgMed": "$stansdato",
                                    "valgteHjemler": []
                                }
                            }
                        """.trimIndent(),
                    )
                }.apply {
                    val bodyAsText = this.bodyAsText()
                    withClue(
                        "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
                    ) {
                        status shouldBe HttpStatusCode.OK
                        val oppdatertRevurdering = objectMapper.readValue<RevurderingDTO>(bodyAsText)
                        oppdatertRevurdering.status shouldBe BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
                    }
                }
            }
        }
    }

    @Test
    fun `send revurdering stans til beslutning feiler hvis stansdato er før innvilgelsesperioden`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurderingStans(tac)

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
                        @Language("JSON")
                        """
                            {
                                "type": "STANS",
                                "begrunnelse": "Begrunnelse",
                                "stans": {
                                    "stansFraOgMed": "$stansdato",
                                    "valgteHjemler": []
                                }
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
    fun `send revurdering stans til beslutning feiler hvis stansdato er etter innvilgelsesperioden`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, _, revurdering) = startRevurderingStans(tac)
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
                        //language=JSON
                        """
                            {
                                "type": "STANS",
                                "begrunnelse": "Begrunnelse",
                                "stans": {
                                    "stansFraOgMed": "$stansdato",
                                    "valgteHjemler": []
                                }
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
    fun `kan sende revurdering med forlenget innvilgelse til beslutning`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val søknadsbehandlingVirkningsperiode = Periode(1.april(2025), 10.april(2025))
                val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L)

                val (_, _, søknadsbehandling, jsonResponse) = sendRevurderingInnvilgelseTilBeslutning(
                    tac,
                    søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
                    revurderingVirkningsperiode = revurderingInnvilgelsesperiode,
                )

                val behandlingDTO = objectMapper.readValue<RevurderingDTO>(jsonResponse)

                behandlingDTO.status shouldBe BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
                behandlingDTO.resultat shouldBe RevurderingResultatDTO.INNVILGELSE

                val revurdering = tac.behandlingContext.behandlingRepo.hent(BehandlingId.fromString(behandlingDTO.id))

                revurdering.shouldBeInstanceOf<Revurdering>()

                revurdering.resultat shouldBe RevurderingResultat.Innvilgelse(
                    valgteTiltaksdeltakelser = revurdering.valgteTiltaksdeltakelser!!,
                    barnetillegg = søknadsbehandling.barnetillegg,
                    antallDagerPerMeldeperiode = søknadsbehandling.antallDagerPerMeldeperiode!!,
                )

                revurdering.virkningsperiode shouldBe revurderingInnvilgelsesperiode
            }
        }
    }
}
