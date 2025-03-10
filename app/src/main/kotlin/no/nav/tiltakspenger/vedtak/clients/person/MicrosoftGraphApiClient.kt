package no.nav.tiltakspenger.vedtak.clients.person

import arrow.core.Either
import arrow.core.flatten
import arrow.core.getOrElse
import arrow.core.right
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.http.toURI
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.felles.NavIdentClient
import no.nav.tiltakspenger.vedtak.felles.sikkerlogg
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private data class MicrosoftGraphResponse(
    val displayName: String,
)

private data class ListOfMicrosoftGraphResponse(
    val value: List<MicrosoftGraphResponse>,
)

class MicrosoftGraphApiClient(
    private val getToken: suspend () -> AccessToken,
    private val timeout: Duration = 1.seconds,
    private val baseUrl: String,
    connectTimeout: Duration = 1.seconds,
) : NavIdentClient {
    private val log = KotlinLogging.logger { }

    /**
     * Denne oppretter en URI med en URLBuilder for at encodingen skal bli riktig for spesialtegn (apostrof ')
     */
    private fun uri(navIdent: String): URI {
        val urlBuilder = URLBuilder().apply {
            protocol = if (Configuration.isNais()) URLProtocol.HTTPS else URLProtocol.HTTP
            host = baseUrl
            encodedPath = "/users"
            parameters.append("\$select", "displayName")
            parameters.append("\$filter", "onPremisesSamAccountName eq '$navIdent'")
            parameters.append("\$count", "true")
        }
        return urlBuilder.build().toURI()
    }

    private val client =
        java.net.http.HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build()

    /**
     * Denne returnerer navnet til saksbehandler eller kaster runtimeException om noe feiler
     */
    override suspend fun hentNavnForNavIdent(navIdent: String): String {
        return hentBrukerinformasjonForNavIdent(navIdent).let { brukerInfo ->
            val saksbehandlersNavn = brukerInfo.displayName.trim()
            if (saksbehandlersNavn.isBlank()) {
                throw RuntimeException("Fant ikke saksbehandlerens navn i microsoftGraphApi $navIdent. Responsen var blank.")
            }
            saksbehandlersNavn
        }
    }

    private suspend fun hentBrukerinformasjonForNavIdent(navIdent: String): MicrosoftGraphResponse {
        return Either.catch {
            val token = getToken()
            val uri = uri(navIdent)
            val request = createRequest(uri, token.token)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val status = httpResponse.statusCode()
            val body = httpResponse.body()
            sikkerlogg.debug { "Logger response fra microsoftGraphApi for å debugge -> $status - $body" }
            if (status == 401 || status == 403) {
                log.error(RuntimeException("Trigger stacktrace for debug.")) { "Invaliderer cache for systemtoken mot Microsoft Graph API. status: $status." }
                token.invaliderCache()
            }
            val jsonResponse = deserialize<ListOfMicrosoftGraphResponse>(body)
            jsonResponse.let { response ->
                if (response.value.size != 1) {
                    log.error("Fant ingen eller flere brukere for navIdent $navIdent: ${response.value.size}. Se sikker logg dersom vi fant flere.")
                    if (response.value.isNotEmpty()) {
                        sikkerlogg.error("Fant ingen eller flere brukere for navIdent $navIdent: ${response.value}")
                    }
                    throw RuntimeException("Fant ikke bruker for navident: $navIdent")
                } else {
                    response.value.first().right()
                }
            }
        }.flatten().getOrElse {
            sikkerlogg.error(it) { "Ukjent feil mot Microsoft Graph Api for bruker: $navIdent message: ${it.message}" }
            throw RuntimeException("Ukjent feil mot Microsoft Graph Api for bruker $navIdent. Se sikker logg for mer context")
        }
    }

    private fun createRequest(
        uri: URI,
        token: String,
    ): HttpRequest {
        return HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
            .header("ConsistencyLevel", "eventual")
            .GET()
            .build()
    }
}
