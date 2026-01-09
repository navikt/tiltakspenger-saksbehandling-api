package no.nav.tiltakspenger.saksbehandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
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

    @Test
    fun `withSakId returns 400 when sakId is invalid`() = testApplication {
        routing {
            get("/test/{sakId}") {
                call.withSakId { sakId ->
                    call.respondJsonString(json = """{"sakId":"$sakId"}""")
                }
            }
        }

        val response = client.get("/test/invalid-uuid")

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Ugyldig sak id","kode":"ugyldig_sak_id"}"""
    }

    @Test
    fun `withSakId calls onRight with valid sakId`() = testApplication {
        val sakId = SakId.random()
        routing {
            get("/test/{sakId}") {
                call.withSakId { sakId ->
                    call.respondJsonString(json = """{"sakId":"$sakId"}""")
                }
            }
        }

        val response = client.get("/test/$sakId")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"sakId":"$sakId"}"""
    }

    @Test
    fun `withSøknadId returns 400 when søknadId is invalid`() = testApplication {
        routing {
            get("/test/{søknadId}") {
                call.withSøknadId { søknadId ->
                    call.respondJsonString(json = """{"søknadId":"$søknadId"}""")
                }
            }
        }

        val response = client.get("/test/invalid-uuid")

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Ugyldig søknad id","kode":"ugyldig_søknad_id"}"""
    }

    @Test
    fun `withSøknadId calls onRight with valid søknadId`() = testApplication {
        val søknadId = SøknadId.random()
        routing {
            get("/test/{søknadId}") {
                call.withSøknadId { søknadId ->
                    call.respondJsonString(json = """{"søknadId":"$søknadId"}""")
                }
            }
        }

        val response = client.get("/test/$søknadId")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"søknadId":"$søknadId"}"""
    }

    @Test
    fun `withMeldekortId returns 400 when meldekortId is invalid`() = testApplication {
        routing {
            get("/test/{meldekortId}") {
                call.withMeldekortId { meldekortId ->
                    call.respondJsonString(json = """{"meldekortId":"$meldekortId"}""")
                }
            }
        }

        val response = client.get("/test/invalid-uuid")

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Ugyldig meldekort id","kode":"ugyldig_meldekort_id"}"""
    }

    @Test
    fun `withMeldekortId calls onRight with valid meldekortId`() = testApplication {
        val meldekortId = MeldekortId.random()
        routing {
            get("/test/{meldekortId}") {
                call.withMeldekortId { meldekortId ->
                    call.respondJsonString(json = """{"meldekortId":"$meldekortId"}""")
                }
            }
        }

        val response = client.get("/test/$meldekortId")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"meldekortId":"$meldekortId"}"""
    }

    @Test
    fun `withMeldeperiodeKjedeId returns 400 when kjedeId is invalid`() = testApplication {
        routing {
            get("/test/{kjedeId}") {
                call.withMeldeperiodeKjedeId { kjedeId ->
                    call.respondJsonString(json = """{"kjedeId":"$kjedeId"}""")
                }
            }
        }

        val response = client.get("/test/invalid")

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Ugyldig meldeperiode-kjede id","kode":"ugyldig_meldeperiodekjede_id"}"""
    }

    @Test
    fun `withMeldeperiodeKjedeId calls onRight with valid kjedeId`() = testApplication {
        routing {
            get("/test/{kjedeId}") {
                call.withMeldeperiodeKjedeId { kjedeId ->
                    call.respondJsonString(json = """{"kjedeId":"$kjedeId"}""")
                }
            }
        }

        val response = client.get("/test/2025-01-06%2F2025-01-19")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"kjedeId":"2025-01-06/2025-01-19"}"""
    }

    @Test
    fun `withMeldeperiodeId returns 400 when meldeperiodeId is invalid`() = testApplication {
        routing {
            get("/test/{meldeperiodeId}") {
                call.withMeldeperiodeId { meldeperiodeId ->
                    call.respondJsonString(json = """{"meldeperiodeId":"$meldeperiodeId"}""")
                }
            }
        }

        val response = client.get("/test/invalid")

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Ugyldig meldeperiode id","kode":"ugyldig_meldeperiode_id"}"""
    }

    @Test
    fun `withMeldeperiodeId calls onRight with valid meldeperiodeId`() = testApplication {
        val meldeperiodeId = MeldeperiodeId.random()
        routing {
            get("/test/{meldeperiodeId}") {
                call.withMeldeperiodeId { meldeperiodeId ->
                    call.respondJsonString(json = """{"meldeperiodeId":"$meldeperiodeId"}""")
                }
            }
        }

        val response = client.get("/test/$meldeperiodeId")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"meldeperiodeId":"$meldeperiodeId"}"""
    }

    @Test
    fun `withBehandlingId returns 400 when behandlingId is invalid`() = testApplication {
        routing {
            get("/test/{behandlingId}") {
                call.withBehandlingId { behandlingId ->
                    call.respondJsonString(json = """{"behandlingId":"$behandlingId"}""")
                }
            }
        }

        val response = client.get("/test/invalid-uuid")

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Ugyldig behandling id","kode":"ugyldig_behandling_id"}"""
    }

    @Test
    fun `withBehandlingId calls onRight with valid behandlingId`() = testApplication {
        val behandlingId = BehandlingId.random()
        routing {
            get("/test/{behandlingId}") {
                call.withBehandlingId { behandlingId ->
                    call.respondJsonString(json = """{"behandlingId":"$behandlingId"}""")
                }
            }
        }

        val response = client.get("/test/$behandlingId")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"behandlingId":"$behandlingId"}"""
    }

    @Test
    fun `withBody returns 400 when body cannot be deserialized`() = testApplication {
        routing {
            post("/test") {
                call.withBody<TestRequestBody> { body ->
                    call.respondJsonString(json = """{"name":"${body.name}"}""")
                }
            }
        }

        val response = client.post("/test") {
            contentType(ContentType.Application.Json)
            setBody("""{"invalid": "json structure"}""")
        }

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Kunne ikke deserialisere request","kode":"ugyldig_request"}"""
    }

    @Test
    fun `withBody returns 400 when body is not valid json`() = testApplication {
        routing {
            post("/test") {
                call.withBody<TestRequestBody> { body ->
                    call.respondJsonString(json = """{"name":"${body.name}"}""")
                }
            }
        }

        val response = client.post("/test") {
            contentType(ContentType.Application.Json)
            setBody("not json at all")
        }

        response.status shouldBe HttpStatusCode.BadRequest
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"melding":"Kunne ikke deserialisere request","kode":"ugyldig_request"}"""
    }

    @Test
    fun `withBody calls ifRight with valid body`() = testApplication {
        routing {
            post("/test") {
                call.withBody<TestRequestBody> { body ->
                    call.respondJsonString(json = """{"name":"${body.name}","age":${body.age}}""")
                }
            }
        }

        val response = client.post("/test") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Test","age":25}""")
        }

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"name":"Test","age":25}"""
    }

    @Test
    fun `respondStatus returns correct status with empty body`() = testApplication {
        routing {
            get("/test") {
                call.respondStatus(HttpStatusCode.Accepted)
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.Accepted
        response.headers["Content-Type"] shouldBe "text/plain; charset=UTF-8"
        response.bodyAsText() shouldBe ""
    }

    @Test
    fun `respondOk returns 200 with empty body`() = testApplication {
        routing {
            get("/test") {
                call.respondOk()
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "text/plain; charset=UTF-8"
        response.bodyAsText() shouldBe ""
    }

    @Test
    fun `respondNoContent returns 204 with empty body`() = testApplication {
        routing {
            get("/test") {
                call.respondNoContent()
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.NoContent
        response.headers["Content-Type"] shouldBe "text/plain; charset=UTF-8"
        response.bodyAsText() shouldBe ""
    }

    @Test
    fun `respondJsonString with plain json-string`() = testApplication {
        routing {
            get("/test") {
                call.respondJsonString(json = """"my-string"""")
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """"my-string""""
    }

    @Test
    fun `respondJsonString with string returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJsonString(json = """{"key":"value"}""")
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"key":"value"}"""
    }

    @Test
    fun `respondJsonString with string and custom status returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJsonString(json = """{"key":"value"}""", HttpStatusCode.Created)
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.Created
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"key":"value"}"""
    }

    @Test
    fun `respondJson with object returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJson(value = TestResponseBody("test", 42))
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"name":"test","value":42}"""
    }

    @Test
    fun `respondJson with object and custom status returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJson(value = TestResponseBody("test", 42), HttpStatusCode.Created)
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.Created
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"name":"test","value":42}"""
    }

    @Test
    fun `respondJson with Pair returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJson(HttpStatusCode.Accepted to TestResponseBody("test", 42))
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.Accepted
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """{"name":"test","value":42}"""
    }

    @Test
    fun `respondJson with list returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJson(listOf(TestResponseBody("a", 1), TestResponseBody("b", 2)))
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """[{"name":"a","value":1},{"name":"b","value":2}]"""
    }

    @Test
    fun `respondJson with list with custom status returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJson(listOf(TestResponseBody("a", 1)), HttpStatusCode.Created)
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.Created
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """[{"name":"a","value":1}]"""
    }

    @Test
    fun `respondJson with empty list returns correct response`() = testApplication {
        routing {
            get("/test") {
                call.respondJson(emptyList<TestResponseBody>())
            }
        }

        val response = client.get("/test")

        response.status shouldBe HttpStatusCode.OK
        response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        response.bodyAsText() shouldEqualJson """[]"""
    }
}

private data class TestRequestBody(
    val name: String,
    val age: Int,
)

private data class TestResponseBody(
    val name: String,
    val value: Int,
)
