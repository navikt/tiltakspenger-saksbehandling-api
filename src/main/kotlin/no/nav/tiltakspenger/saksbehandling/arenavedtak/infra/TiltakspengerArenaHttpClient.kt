package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class TiltakspengerArenaHttpClient(
    baseUrl: String,
    val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 1.seconds,
) : TiltakspengerArenaClient {
    private val log = KotlinLogging.logger {}

    private val client = HttpClient
        .newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    private val uri = URI.create("$baseUrl/azure/tiltakspenger/vedtak")

    override suspend fun hentTiltakspengevedtakFraArena(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): List<ArenaTPVedtak> {
        try {
            val jsonPayload = serialize(
                VedtakRequest(
                    ident = fnr.verdi,
                    fom = periode.fraOgMed,
                    tom = periode.tilOgMed,
                ),
            )
            val request = createPostRequest(jsonPayload, getToken().token)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val status = httpResponse.statusCode()
            val jsonResponse = httpResponse.body()
            if (status != 200) {
                log.error { "Kunne ikke hente vedtak fra arena for correlationId $correlationId, statuskode $status." }
                Sikkerlogg.error { "Feil mot tiltakspenger-arena for correlationId $correlationId: Request: $jsonPayload, response: $jsonResponse" }
                error("Kunne ikke hente vedtak fra arena, statuskode $status")
            }
            return objectMapper.readValue<List<ArenaTPVedtak>>(jsonResponse)
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved henting av tiltakspengevedtak fra Arena: ${e.message}" }
            throw e
        }
    }

    private fun createPostRequest(
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
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}

data class VedtakRequest(
    val ident: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
)
