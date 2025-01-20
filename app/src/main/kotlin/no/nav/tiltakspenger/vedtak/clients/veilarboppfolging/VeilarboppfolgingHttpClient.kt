package no.nav.tiltakspenger.vedtak.clients.veilarboppfolging

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.ports.VeilarboppfolgingGateway
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
) : VeilarboppfolgingGateway {
    private val log = KotlinLogging.logger {}
    private val client =
        java.net.http.HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build()

    private val uri = URI.create("$baseUrl/veilarboppfolging/graphql")

    override suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        val jsonPayload = objectMapper.writeValueAsString(hentOppfolgingsenhetQuery(fnr))
        val request = createRequest(jsonPayload, getToken().token)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 200) {
            log.error("Kunne ikke hente oppfølgingsenhet fra veilarboppfølging, statuskode $status")
            error("Kunne ikke hente oppfølgingsenhet fra veilarboppfølging, statuskode $status")
        }
        val jsonResponse = httpResponse.body()
        val response = objectMapper.readValue<HentOppfolgingsenhetResponse>(jsonResponse)
        if (response.errors.isNotEmpty()) {
            response.errors.forEach { log.warn(it) }
        }
        if (response.data == null) {
            log.error("Respons fra veilarboppfølging mangler data")
            error("Respons fra veilarboppfølging mangler data")
        }
        val oppfolgingsenhet = response.data.oppfolgingsEnhet.enhet
        return oppfolgingsenhet?.toNavkontor() ?: error("Fant ikke oppfølgingsenhet")
    }

    private fun createRequest(
        jsonPayload: String,
        token: String,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
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
