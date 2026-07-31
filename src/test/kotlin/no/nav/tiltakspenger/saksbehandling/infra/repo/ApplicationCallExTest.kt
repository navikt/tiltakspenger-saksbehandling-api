package no.nav.tiltakspenger.saksbehandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.ktor.common.respondJsonString
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.withSaksnummer
import org.junit.jupiter.api.Test

class ApplicationCallExTest {

    @Test
    fun `correlationId returns CorrelationId from callId when present`() = testApplication {
        install(CallId) {
            header("X-Correlation-ID")
        }
        routing {
            get("/test") {
                val correlationId = call.correlationId()
                call.respondJsonString(json = """{"correlationId":"${correlationId.value}"}""")
            }
        }

        val response = client.get("/test") {
            headers.append("X-Correlation-ID", "test-correlation-id")
        }

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"correlationId":"test-correlation-id"}"""
    }

    @Test
    fun `correlationId generates new CorrelationId when callId is null`() = testApplication {
        routing {
            get("/test") {
                val correlationId = call.correlationId()
                call.respondJsonString(json = """{"hasCorrelationId":${correlationId.value.isNotBlank()}}""")
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"hasCorrelationId":true}"""
    }

    @Test
    fun `withSaksnummer returns 400 when saksnummer is missing`() = testApplication {
        routing {
            get("/test/{saksnummer}") {
                call.withSaksnummer { saksnummer ->
                    call.respondJsonString(json = """{"saksnummer":"${saksnummer.verdi}"}""")
                }
            }
        }

        val response = client.get("/test/invalid")

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Ugyldig saksnummer","kode":"ugyldig_saksnummer"}"""
    }

    @Test
    fun `withSaksnummer calls onRight with valid saksnummer`() = testApplication {
        routing {
            get("/test/{saksnummer}") {
                call.withSaksnummer { saksnummer ->
                    call.respondJsonString(json = """{"saksnummer":"${saksnummer.verdi}"}""")
                }
            }
        }

        val response = client.get("/test/202501011001")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"saksnummer":"202501011001"}"""
    }
}
