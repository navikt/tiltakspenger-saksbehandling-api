package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.VeilarboppfolgingKlient
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class VeilarboppfolgingHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    connectTimeout: kotlin.time.Duration = 1.seconds,
    private val timeout: kotlin.time.Duration = 1.seconds,
) : VeilarboppfolgingKlient {
    private val logger = KotlinLogging.logger {}
    private val client =
        java.net.http.HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build()

    private val uri = URI.create("$baseUrl/veilarboppfolging/api/v2/person/system/hent-oppfolgingsstatus")

    override suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return withContext(Dispatchers.IO) {
            val jsonPayload = objectMapper.writeValueAsString(Request(fnr.verdi))
            val request = createRequest(jsonPayload, getToken().token)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val status = httpResponse.statusCode()
            if (status != 200) {
                logger.error { "Kunne ikke hente oppfølgingsenhet fra veilarboppfølging, statuskode $status" }
                error("Kunne ikke hente oppfølgingsenhet fra veilarboppfølging, statuskode $status")
            }
            val jsonResponse = httpResponse.body()
            val oppfolgingsenhet = objectMapper.readValue<Response>(jsonResponse).oppfolgingsenhet
            if (oppfolgingsenhet == null) {
                logger.error { "Fant ikke oppfølgingsenhet" }
            }
            oppfolgingsenhet?.toNavkontor() ?: error("Fant ikke oppfølgingsenhet")
        }
    }

    private fun createRequest(
        jsonPayload: String,
        token: String,
    ): HttpRequest? {
        return HttpRequest.newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Nav-Consumer-Id", "tiltakspenger-saksbehandling-api")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}

private data class Request(
    val fnr: String,
)

private data class Response(
    val oppfolgingsenhet: Oppfolgingsenhet?,
)

private data class Oppfolgingsenhet(
    val navn: String,
    val enhetId: String,
) {
    fun toNavkontor(): Navkontor =
        Navkontor(
            kontornummer = enhetId,
            kontornavn = navn,
        )
}
