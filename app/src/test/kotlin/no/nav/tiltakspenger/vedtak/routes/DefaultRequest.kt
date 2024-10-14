package no.nav.tiltakspenger.vedtak.routes

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.append
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.common.JwtAndJwkGenerator

suspend fun ApplicationTestBuilder.defaultRequest(
    method: HttpMethod,
    uri: String,
    jwt: String = JwtAndJwkGenerator.createJwkJwtPairForSaksbehandler().second,
    setup: HttpRequestBuilder.() -> Unit = {},
): HttpResponse =
    this.client.request(uri) {
        this.method = method
        this.headers {
            append(HttpHeaders.XCorrelationId, "DEFAULT_CALL_ID")
            append(HttpHeaders.ContentType, ContentType.Application.Json)
            append(HttpHeaders.Authorization, "Bearer $jwt")
        }
        setup()
    }
