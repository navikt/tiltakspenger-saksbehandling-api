package no.nav.tiltakspenger.saksbehandling.person.infra.http

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import org.junit.jupiter.api.Test

/**
 * Oppslaget av adressebeskyttelse mot PDL, over HTTP.
 * Feilene logges av route-laget, så testene sjekker konteksten feilen bærer: ingen fødselsnumre i vanlig logg, og requesten med identene i sikkerlogg.
 */
class PersonHttpklientTest {

    private val fnr = Fnr.random()

    private fun klient(baseUrl: String) = PersonHttpklient(
        endepunkt = "$baseUrl/graphql",
        clock = fixedClock,
        getToken = { ObjectMother.accessToken() },
    )

    @Test
    fun `gir graderingen per person`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/graphql"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = """
                    {
                      "data": {
                        "hentPersonBolk": [
                          { "ident": "${fnr.verdi}", "person": { "adressebeskyttelse": [] }, "code": "ok" }
                        ]
                      }
                    }
                """.trimIndent()
            }

            runTest {
                klient(wiremock.baseUrl()).hentAdressebeskyttelse(listOf(fnr)).getOrNull() shouldBe
                    mapOf(fnr to Adressebeskyttelse.UGRADERT)
            }
        }
    }

    @Test
    fun `uventet status gir feil med HTTP-kontekst, og identene bare i sikkerloggkonteksten`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/graphql"
            } returns {
                statusCode = 500
            }

            runTest {
                val feil = klient(wiremock.baseUrl()).hentAdressebeskyttelse(listOf(fnr)).leftOrNull()!!

                feil.loggkontekst.melding shouldContain "Uventet HTTP-status 500"
                feil.loggkontekst.melding shouldContain "1 personer"
                feil.loggkontekst.melding shouldNotContain fnr.verdi
                feil.sikkerloggkontekst!!.melding shouldContain fnr.verdi
            }
        }
    }

    @Test
    fun `feil fra PDL i svaret navngis uten innholdet i vanlig logg`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/graphql"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = """
                    {
                      "errors": [
                        {
                          "message": "Ikke tilgang til ${fnr.verdi}",
                          "locations": [ { "line": 2, "column": 5 } ],
                          "path": [ "hentPersonBolk" ],
                          "extensions": { "code": "unauthorized", "classification": "ExecutionAborted" }
                        }
                      ]
                    }
                """.trimIndent()
            }

            runTest {
                val feil = klient(wiremock.baseUrl()).hentAdressebeskyttelse(listOf(fnr)).leftOrNull()!!

                feil.loggkontekst.melding shouldContain "UkjentFeil"
                feil.loggkontekst.melding shouldNotContain fnr.verdi
                feil.sikkerloggkontekst!!.melding shouldContain fnr.verdi
            }
        }
    }

    @Test
    fun `et svar som ikke kan leses, gir feil med svaret bare i sikkerloggkonteksten`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/graphql"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = """{ "data": { "hentPersonBolk": "${fnr.verdi}" } }"""
            }

            runTest {
                val feil = klient(wiremock.baseUrl()).hentAdressebeskyttelse(listOf(fnr)).leftOrNull()!!

                feil.loggkontekst.melding shouldContain "Kunne ikke lese svaret"
                feil.loggkontekst.melding shouldNotContain fnr.verdi
                feil.sikkerloggkontekst!!.melding shouldContain fnr.verdi
            }
        }
    }
}
