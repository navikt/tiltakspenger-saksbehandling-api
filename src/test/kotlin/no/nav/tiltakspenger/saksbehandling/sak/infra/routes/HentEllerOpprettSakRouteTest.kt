package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.infra.route.FnrDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test

class HentEllerOpprettSakRouteTest {
    private val ident = Fnr.random()
    private val systembruker = ObjectMother.systembrukerHentEllerOpprettSak()

    @Test
    fun `hentEllerOpprettSak - sak finnes ikke - oppretter sak`() {
        withTestApplicationContext { tac ->
            val jwt = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("hent_eller_opprett_sak"),
            )
            tac.leggTilBruker(jwt, systembruker)
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path(SAKSNUMMER_PATH)
                },
                jwt = jwt,
            ) {
                setBody(objectMapper.writeValueAsString(FnrDTO(ident.verdi)))
            }.apply {
                status shouldBe HttpStatusCode.OK
            }
            tac.sakContext.sakRepo.hentForFnr(ident).saker shouldNotBe emptyList<Sak>()
        }
    }

    @Test
    fun `hentEllerOpprettSak - sak finnes - returnerer eksisterende sak`() {
        withTestApplicationContext { tac ->
            val sak = ObjectMother.nySak(fnr = ident)
            tac.sakContext.sakRepo.opprettSak(sak)
            val jwt = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("hent_eller_opprett_sak"),
            )
            tac.leggTilBruker(jwt, systembruker)
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path(SAKSNUMMER_PATH)
                },
                jwt = jwt,
            ) {
                setBody(objectMapper.writeValueAsString(FnrDTO(ident.verdi)))
            }.apply {
                status shouldBe HttpStatusCode.OK
                val response = objectMapper.readValue<SaksnummerResponse>(bodyAsText())
                response.saksnummer shouldBe sak.saksnummer.verdi
            }
        }
    }

    @Test
    fun `hentEllerOpprettSak - feil rolle - returnerer 403`() {
        withTestApplicationContext { tac ->
            val sak = ObjectMother.nySak(fnr = ident)
            tac.sakContext.sakRepo.opprettSak(sak)
            val jwt = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("lagre_meldekort"),
            )
            tac.leggTilBruker(jwt, ObjectMother.systembrukerLagreMeldekort())
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path(SAKSNUMMER_PATH)
                },
                jwt = jwt,
            ) {
                setBody(objectMapper.writeValueAsString(FnrDTO(ident.verdi)))
            }.apply {
                status shouldBe HttpStatusCode.Forbidden
            }
        }
    }
}
