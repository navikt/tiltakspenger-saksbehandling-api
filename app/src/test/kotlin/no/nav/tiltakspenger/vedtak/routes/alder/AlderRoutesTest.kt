package no.nav.tiltakspenger.vedtak.routes.alder

import com.fasterxml.jackson.databind.ObjectMapper
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
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.common.Rolle
import no.nav.tiltakspenger.libs.common.Roller
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingUavklart
import no.nav.tiltakspenger.objectmothers.nySøknad
import no.nav.tiltakspenger.vedtak.clients.defaultObjectMapper
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.behandling.benk.behandlingBenkRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder.AlderVilkårDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder.alderRoutes
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import org.junit.jupiter.api.Test

class AlderRoutesTest {

    private val objectMapper: ObjectMapper = defaultObjectMapper()
    private val saksbehandler =
        Saksbehandler(
            "Q123456",
            "Superman",
            "a@b.c",
            Roller(listOf(Rolle.SAKSBEHANDLER, Rolle.SKJERMING, Rolle.STRENGT_FORTROLIG_ADRESSE)),
        )

    @Test
    fun `test at endepunkt for henting og lagring av alder fungerer`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = this.førstegangsbehandlingUavklart()
            val behandlingId = sak.førstegangsbehandling.id

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        alderRoutes(
                            behandlingService = tac.førstegangsbehandlingContext.behandlingService,
                            auditService = tac.personContext.auditService,
                            tokenService = tac.tokenService,
                        )
                    }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/$behandlingId/vilkar/alder")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val alderVilkår = objectMapper.readValue<AlderVilkårDTO>(bodyAsText())
                    alderVilkår.samletUtfall shouldBe SamletUtfallDTO.OPPFYLT
                }
            }
        }
    }

    @Test
    fun `test at søknaden blir gjenspeilet i alder vilkåret`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = this.førstegangsbehandlingUavklart(fødselsdato = 5.januar(2000))
            val behandlingId = sak.førstegangsbehandling.id

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        alderRoutes(
                            tokenService = tac.tokenService,
                            behandlingService = tac.førstegangsbehandlingContext.behandlingService,
                            auditService = tac.personContext.auditService,
                        )
                    }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/$behandlingId/vilkar/alder")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val alderVilkår = objectMapper.readValue<AlderVilkårDTO>(bodyAsText())
                    alderVilkår.samletUtfall shouldBe SamletUtfallDTO.OPPFYLT
                }
            }
        }
    }

    @Test
    fun `test at behandlingen ikke kan opprettes om bruker fyller 18 år midt i vurderingsperioden`() = runTest {
        val vurderingsperiode = Periode(fraOgMed = 1.januar(2018), tilOgMed = 10.januar(2018))
        val fødselsdato = 5.januar(2000)
        with(TestApplicationContext()) {
            val tac = this
            val søknad = this.nySøknad(
                periode = vurderingsperiode,
                personopplysningerForBrukerFraPdl = ObjectMother.personopplysningKjedeligFyr(fødselsdato = fødselsdato),
            )

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        behandlingBenkRoutes(
                            tokenService = tac.tokenService,
                            behandlingService = tac.førstegangsbehandlingContext.behandlingService,
                            sakService = tac.sakContext.sakService,
                            auditService = tac.personContext.auditService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/startbehandling")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ) {
                    setBody("""{"id":"${søknad.id}"}""")
                }.apply {
                    withClue(
                        "Response details:\n" +
                            "Status: ${this.status}\n" +
                            "Content-Type: ${this.contentType()}\n" +
                            "Body: ${this.bodyAsText()}\n",
                    ) {
                        status shouldBe HttpStatusCode.BadRequest
                        bodyAsText() shouldBe """
                            {"melding":"Vi støtter ikke delvis innvilgelse eller avslag.","kode":"støtter_ikke_delvis_innvilgelse_eller_avslag"}
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
