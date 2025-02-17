package no.nav.tiltakspenger.vedtak.clients.pdfgen

import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.KunneIkkeGenererePdf
import no.nav.tiltakspenger.felles.PdfA
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.Navn
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Har ansvar for å konvertere domene til JSON som sendes til https://github.com/navikt/tiltakspenger-pdfgen for å generere PDF.
 *
 * timeout er satt til 6 sekunder siden pdfgen bruker lang tid første gang den genererer en pdf (nesten 5 sekunder). Etter det tar det 1-2 sekunder
 */
internal class PdfgenHttpClient(
    baseUrl: String,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 6.seconds,
) : GenererInnvilgelsesvedtaksbrevGateway,
    GenererUtbetalingsvedtakGateway,
    GenererStansvedtaksbrevGateway {

    private val log = KotlinLogging.logger {}

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    private val vedtakInnvilgelseUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakInnvilgelse")
    private val utbetalingsvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/utbetalingsvedtak")
    private val stansvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/revurderingsvedtak")

    override suspend fun genererInnvilgelsesvedtaksbrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                vedtak.toInnvilgetSøknadsbrev(hentBrukersNavn, hentSaksbehandlersNavn, vedtaksdato)
            },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}, vedtakId: ${vedtak.id}",
            uri = vedtakInnvilgelseUri,
        )
    }

    override suspend fun genererInnvilgelsesvedtaksbrevMedTilleggstekst(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        tilleggstekst: String?,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                vedtak.toInnvilgetSøknadsbrev(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    vedtaksdato = vedtaksdato,
                    tilleggstekst = tilleggstekst,
                )
            },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}, vedtakId: ${vedtak.id}",
            uri = vedtakInnvilgelseUri,
        )
    }

    override suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltaksnavn: String,
        eksternGjennomføringId: String?,
        eksternDeltagelseId: String,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                utbetalingsvedtak.toJsonRequest(
                    hentSaksbehandlersNavn,
                    tiltaksnavn,
                    eksternGjennomføringId,
                    eksternDeltagelseId,
                )
            },
            errorContext = "SakId: ${utbetalingsvedtak.sakId}, saksnummer: ${utbetalingsvedtak.saksnummer}, vedtakId: ${utbetalingsvedtak.id}",
            uri = utbetalingsvedtakUri,
        )
    }

    override suspend fun genererStansvedtak(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = { vedtak.toRevurderingStans(hentBrukersNavn, hentSaksbehandlersNavn, vedtaksdato) },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}, vedtakId: ${vedtak.id}",
            uri = stansvedtakUri,
        )
    }

    private suspend fun pdfgenRequest(
        jsonPayload: suspend () -> String,
        errorContext: String,
        uri: URI,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return withContext(Dispatchers.IO) {
            Either.catch {
                val request = createPdfgenRequest(jsonPayload(), uri)
                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).await()
                val jsonResponse = httpResponse.body()
                val status = httpResponse.statusCode()
                if (status != 200) {
                    log.error { "Feil ved kall til pdfgen. $errorContext. Status: $status. uri: $uri. Se sikkerlogg for detaljer." }
                    sikkerlogg.error { "Feil ved kall til pdfgen. $errorContext. uri: $uri. jsonResponse: $jsonResponse. jsonPayload: $jsonPayload." }
                    return@withContext KunneIkkeGenererePdf.left()
                }
                PdfOgJson(PdfA(jsonResponse), jsonPayload())
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                log.error(it) { "Feil ved kall til pdfgen. $errorContext. Se sikkerlogg for detaljer." }
                sikkerlogg.error(it) { "Feil ved kall til pdfgen. $errorContext. jsonPayload: $jsonPayload, uri: $uri" }
                KunneIkkeGenererePdf
            }
        }
    }

    private fun createPdfgenRequest(
        jsonPayload: String,
        uri: URI,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Accept", "application/pdf")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}
