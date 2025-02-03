package no.nav.tiltakspenger.vedtak.routes.sak

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import org.junit.jupiter.api.Test

class HentEllerOpprettSakRouteTest {
    private val ident = Fnr.random()

    @Test
    fun `hentEllerOpprettSak - sak finnes ikke - oppretter sak`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        hentEllerOpprettSakRoute(
                            sakService = tac.sakContext.sakService,
                            tokenService = tac.tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path(SAKSNUMMER_PATH)
                    },
                    jwt = tac.jwtGenerator.createJwtForSystembruker(
                        roles = listOf("lage_hendelser"),
                    ),
                ) {
                    setBody(objectMapper.writeValueAsString(FnrDTO(ident.verdi)))
                }.apply {
                    status shouldBe HttpStatusCode.OK
                }
            }
            sakContext.sakRepo.hentForFnr(ident).saker shouldNotBe emptyList<Sak>()
        }
    }

    @Test
    fun `hentEllerOpprettSak - sak finnes - returnerer eksisterende sak`() {
        with(TestApplicationContext()) {
            val tac = this
            val sak = ObjectMother.nySak(fnr = ident)
            tac.sakContext.sakRepo.opprettSak(sak)
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        hentEllerOpprettSakRoute(
                            sakService = tac.sakContext.sakService,
                            tokenService = tac.tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path(SAKSNUMMER_PATH)
                    },
                    jwt = tac.jwtGenerator.createJwtForSystembruker(
                        roles = listOf("lage_hendelser"),
                    ),
                ) {
                    setBody(objectMapper.writeValueAsString(FnrDTO(ident.verdi)))
                }.apply {
                    status shouldBe HttpStatusCode.OK
                    val response = objectMapper.readValue<SaksnummerResponse>(bodyAsText())
                    response.saksnummer shouldBe sak.saksnummer.verdi
                }
            }
        }
    }
}
