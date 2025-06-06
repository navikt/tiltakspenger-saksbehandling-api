package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
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
    private val timeout: Duration = 20.seconds,
) : GenererVedtaksbrevForInnvilgelseKlient,
    GenererVedtaksbrevForUtbetalingKlient,
    GenererVedtaksbrevForStansKlient,
    GenererVedtaksbrevForAvslagKlient {

    private val log = KotlinLogging.logger {}

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    private val vedtakInnvilgelseUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakInnvilgelse")
    private val vedtakAvslagUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakAvslag")
    private val utbetalingsvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/utbetalingsvedtak")
    private val stansvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/stansvedtak")
    private val revurderingsvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/revurderingsvedtak")

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
        tilleggstekst: FritekstTilVedtaksbrev?,
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

    override suspend fun genererInnvilgelsesvedtaksbrevMedTilleggstekst(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        tilleggstekst: FritekstTilVedtaksbrev?,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        innvilgelsesperiode: Periode,
        saksnummer: Saksnummer,
        sakId: SakId,
        forhåndsvisning: Boolean,
        barnetilleggsPerioder: Periodisering<AntallBarn>?,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                genererInnvilgetSøknadsbrev(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    vedtaksdato = vedtaksdato,
                    tilleggstekst = tilleggstekst,
                    fnr = fnr,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    beslutterNavIdent = beslutterNavIdent,
                    innvilgelsesperiode = innvilgelsesperiode,
                    saksnummer = saksnummer,
                    forhåndsvisning = forhåndsvisning,
                    barnetilleggsPerioder = barnetilleggsPerioder,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = vedtakInnvilgelseUri,
        )
    }

    override suspend fun genererInnvilgetRevurderingBrev(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        saksnummer: Saksnummer,
        sakId: SakId,
        forhåndsvisning: Boolean,
        vurderingsperiode: Periode,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                genererRevurderingInnvilgetBrev(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    fnr = fnr,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    beslutterNavIdent = beslutterNavIdent,
                    saksnummer = saksnummer,
                    forhåndsvisning = forhåndsvisning,
                    vurderingsperiode = vurderingsperiode,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = revurderingsvedtakUri,
        )
    }

    override suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltaksdeltagelser: List<Tiltaksdeltagelse>,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                utbetalingsvedtak.toJsonRequest(
                    hentSaksbehandlersNavn,
                    tiltaksdeltagelser,
                    sammenligning,
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
            jsonPayload = {
                vedtak.toRevurderingStans(
                    hentBrukersNavn,
                    hentSaksbehandlersNavn,
                    vedtaksdato,
                )
            },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}, vedtakId: ${vedtak.id}",
            uri = stansvedtakUri,
        )
    }

    override suspend fun genererStansvedtak(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        virkningsperiode: Periode,
        saksnummer: Saksnummer,
        sakId: SakId,
        forhåndsvisning: Boolean,
        tilleggstekst: FritekstTilVedtaksbrev?,
        barnetillegg: Boolean,
        valgtHjemmelHarIkkeRettighet: List<ValgtHjemmelHarIkkeRettighet>,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                genererStansbrev(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    vedtaksdato = vedtaksdato,
                    fnr = fnr,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    beslutterNavIdent = beslutterNavIdent,
                    virkningsperiode = virkningsperiode,
                    saksnummer = saksnummer,
                    forhåndsvisning = forhåndsvisning,
                    barnetillegg = barnetillegg,
                    valgteHjemler = valgtHjemmelHarIkkeRettighet,
                    tilleggstekst = tilleggstekst,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = stansvedtakUri,
        )
    }

    override suspend fun genererAvslagsVedtaksbrev(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        avslagsperiode: Periode,
        saksnummer: Saksnummer,
        sakId: SakId,
        tilleggstekst: FritekstTilVedtaksbrev,
        forhåndsvisning: Boolean,
        harSøktBarnetillegg: Boolean,
        datoForUtsending: LocalDate,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                genererAvslagSøknadsbrev(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    tilleggstekst = tilleggstekst,
                    fnr = fnr,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    beslutterNavIdent = beslutterNavIdent,
                    saksnummer = saksnummer,
                    forhåndsvisning = forhåndsvisning,
                    avslagsgrunner = avslagsgrunner,
                    harSøktBarnetillegg = harSøktBarnetillegg,
                    avslagsperiode = avslagsperiode,
                    datoForUtsending = datoForUtsending,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = vedtakAvslagUri,
        )
    }

    override suspend fun genererAvslagsvVedtaksbrev(
        vedtak: Rammevedtak,
        datoForUtsending: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                vedtak.genererAvslagSøknadsbrev(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    datoForUtsending = datoForUtsending,
                )
            },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}",
            uri = vedtakAvslagUri,
        )
    }

    private suspend fun pdfgenRequest(
        jsonPayload: suspend () -> String,
        errorContext: String,
        uri: URI,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return withContext(Dispatchers.IO) {
            val payload = jsonPayload()
            Either.catch {
                val request = createPdfgenRequest(payload, uri)
                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).await()
                val jsonResponse = httpResponse.body()
                val status = httpResponse.statusCode()
                if (status != 200) {
                    log.error { "Feil ved kall til pdfgen. $errorContext. Status: $status. uri: $uri. Se sikkerlogg for detaljer." }
                    Sikkerlogg.error { "Feil ved kall til pdfgen. $errorContext. uri: $uri. jsonResponse: $jsonResponse. jsonPayload: $payload." }
                    return@withContext KunneIkkeGenererePdf.left()
                }
                PdfOgJson(PdfA(jsonResponse), payload)
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                log.error(it) { "Feil ved kall til pdfgen. $errorContext. Se sikkerlogg for detaljer." }
                Sikkerlogg.error(it) { "Feil ved kall til pdfgen. $errorContext. jsonPayload: $payload, uri: $uri" }
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
