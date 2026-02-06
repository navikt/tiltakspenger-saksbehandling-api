package no.nav.tiltakspenger.saksbehandling.infra.http

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import javax.net.ssl.SSLSession

class HttpExTest {
    @Test
    fun `responskoder mellom 200 or 299 er success`() {
        createResponseWithStatusCode(199).isSuccess() shouldBe false
        createResponseWithStatusCode(200).isSuccess() shouldBe true
        createResponseWithStatusCode(299).isSuccess() shouldBe true
        createResponseWithStatusCode(300).isSuccess() shouldBe false
    }

    private fun createResponseWithStatusCode(statusCode: Int): HttpResponse<String> {
        return object : HttpResponse<String> {
            override fun statusCode(): Int = statusCode

            // er ikke viktig for testen, men må overrides
            override fun request(): HttpRequest? = null
            override fun previousResponse(): Optional<HttpResponse<String?>?>? = null
            override fun headers(): HttpHeaders? = null
            override fun body(): String? = null
            override fun sslSession(): Optional<SSLSession?>? = null
            override fun uri(): URI? = null
            override fun version(): HttpClient.Version? = null
        }
    }
}
