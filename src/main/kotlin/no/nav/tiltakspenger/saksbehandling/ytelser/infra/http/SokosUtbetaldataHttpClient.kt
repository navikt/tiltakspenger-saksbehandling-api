package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

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
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

// swagger: https://sokos-utbetaldata.dev.intern.nav.no/utbetaldata/api/v2/docs
class SokosUtbetaldataHttpClient(
    baseUrl: String,
    val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 1.seconds,
) : SokosUtbetaldataClient {
    private val log = KotlinLogging.logger {}

    // utbetaldata sender beskrivelsen, ikke kodeverdien
    private val relevanteYtelsestyper = Ytelsetype.entries.toTypedArray().map { it.tekstverdi }

    private val client = HttpClient
        .newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    private val utbetalingsinformasjonUri = URI.create("$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")

    override suspend fun hentYtelserFraUtbetaldata(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): List<Ytelse> {
        val jsonPayload = serialize(
            HentUtbetalingsinformasjonRequest(
                ident = fnr.verdi,
                periode = UtbetalingDto.Periode(
                    fom = periode.fraOgMed,
                    tom = LocalDate.now(), // utbetaldata godtar ikke datoer frem i tid
                ),
            ),
        )
        val request = createPostRequest(jsonPayload, getToken().token, correlationId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        val jsonResponse = httpResponse.body()
        if (status != 200) {
            log.error { "Kunne ikke hente utbetalingsinformasjon, statuskode $status. CorrelationId: $correlationId" }
            Sikkerlogg.error { "Feil mot utbetaldata: Request: $jsonPayload, response: $jsonResponse" }
            error("Kunne ikke hente utbetalingsinformasjon, statuskode $status")
        }
        val utbetalinger = objectMapper.readValue<List<UtbetalingDto>>(jsonResponse)
        val ytelser = utbetalinger.tilYtelser()
        log.info { "Fant ${ytelser.size} relevante ytelser for correlationId $correlationId" }
        return ytelser
    }

    private fun List<UtbetalingDto>.tilYtelser(): List<Ytelse> {
        val utbetalinger = this.flatMap { it.ytelseListe }
        val relevanteYtelser = utbetalinger.filter { it.ytelsestype == null || it.ytelsestype in relevanteYtelsestyper }
            .groupBy { it.ytelsestype }
        return relevanteYtelser.map { ytelse ->
            Ytelse(
                ytelsetype = ytelse.key?.let { key -> Ytelsetype.entries.find { key == it.tekstverdi } }
                    ?: Ytelsetype.UKJENT,
                perioder = ytelse.value.map {
                    Periode(
                        fraOgMed = it.ytelsesperiode.fom,
                        tilOgMed = it.ytelsesperiode.tom,
                    )
                },
            )
        }
    }

    private fun createPostRequest(
        jsonPayload: String,
        token: String,
        callId: CorrelationId,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(utbetalingsinformasjonUri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("nav-call-id", callId.toString())
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}
