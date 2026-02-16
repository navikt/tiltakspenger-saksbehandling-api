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
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.dato.norskTidspunktFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForOpphørKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlign
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.ports.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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
class PdfgenHttpClient(
    baseUrl: String,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 20.seconds,
) : GenererVedtaksbrevForInnvilgelseKlient,
    GenererVedtaksbrevForUtbetalingKlient,
    GenererVedtaksbrevForStansKlient,
    GenererVedtaksbrevForAvslagKlient,
    GenererVedtaksbrevForOpphørKlient,
    GenererKlagebrevKlient {

    private val log = KotlinLogging.logger {}

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    private val vedtakInnvilgelseUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakInnvilgelse")
    private val vedtakAvslagUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakAvslag")
    private val meldekortvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/utbetalingsvedtak")
    private val stansvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/stansvedtak")
    private val opphørUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakOpphør")
    private val revurderingInnvilgelseUri = URI.create("$baseUrl/api/v1/genpdf/tpts/revurderingInnvilgelse")
    private val klageAvvisUri = URI.create("$baseUrl/api/v1/genpdf/tpts/klageAvvis")

    override suspend fun genererInnvilgetVedtakBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        tilleggstekst: FritekstTilVedtaksbrev?,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                when (vedtak.rammebehandling) {
                    is Revurdering -> vedtak.tilRevurderingInnvilgetBrev(
                        hentBrukersNavn = hentBrukersNavn,
                        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                        vedtaksdato = vedtaksdato,
                        tilleggstekst = tilleggstekst,
                    )

                    is Søknadsbehandling -> vedtak.tilInnvilgetSøknadsbrev(
                        hentBrukersNavn = hentBrukersNavn,
                        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                        vedtaksdato = vedtaksdato,
                        tilleggstekst = tilleggstekst,
                    )
                }
            },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}, vedtakId: ${vedtak.id}",
            uri = when (vedtak.rammebehandling) {
                is Revurdering -> revurderingInnvilgelseUri
                is Søknadsbehandling -> vedtakInnvilgelseUri
            },
        )
    }

    override suspend fun genererInnvilgetSøknadBrevForhåndsvisning(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        saksnummer: Saksnummer,
        sakId: SakId,
        innvilgelsesperioder: Innvilgelsesperioder,
        barnetilleggsperioder: Periodisering<AntallBarn>?,
        tilleggstekst: FritekstTilVedtaksbrev?,
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
                    saksnummer = saksnummer,
                    forhåndsvisning = true,
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetillegg = barnetilleggsperioder,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = vedtakInnvilgelseUri,
        )
    }

    override suspend fun genererInnvilgetRevurderingBrevForhåndsvisning(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        saksnummer: Saksnummer,
        sakId: SakId,
        innvilgelsesperioder: Innvilgelsesperioder,
        barnetilleggsperioder: Periodisering<AntallBarn>?,
        tilleggstekst: FritekstTilVedtaksbrev?,
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
                    forhåndsvisning = true,
                    innvilgelsesperioder = innvilgelsesperioder,
                    tilleggstekst = tilleggstekst,
                    barnetillegg = barnetilleggsperioder,
                    vedtaksdato = vedtaksdato,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = revurderingInnvilgelseUri,
        )
    }

    override suspend fun genererMeldekortvedtakBrev(
        meldekortvedtak: Meldekortvedtak,
        tiltaksdeltakelser: Tiltaksdeltakelser,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
        forhåndsvisning: Boolean,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                meldekortvedtak.toJsonRequest(
                    hentSaksbehandlersNavn,
                    tiltaksdeltakelser,
                    sammenligning,
                    forhåndsvisning,
                )
            },
            errorContext = "SakId: ${meldekortvedtak.sakId}, saksnummer: ${meldekortvedtak.saksnummer}, vedtakId: ${meldekortvedtak.id}",
            uri = meldekortvedtakUri,
        )
    }

    override suspend fun genererMeldekortvedtakBrev(
        command: GenererMeldekortVedtakBrevCommand,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                BrevMeldekortvedtakDTO(
                    fødselsnummer = command.fnr.verdi,
                    saksbehandler = command.saksbehandler?.tilSaksbehandlerDto(hentSaksbehandlersNavn),
                    beslutter = command.beslutter?.tilSaksbehandlerDto(hentSaksbehandlersNavn),
                    meldekortId = command.meldekortbehandlingId.toString(),
                    saksnummer = command.saksnummer.verdi,
                    meldekortPeriode = command.beregningsperiode?.let {
                        BrevMeldekortvedtakDTO.PeriodeDTO(
                            fom = it.fraOgMed.format(norskDatoFormatter),
                            tom = it.tilOgMed.format(norskDatoFormatter),
                        )
                    },
                    tiltak = command.tiltaksdeltakelser.map { it.toTiltakDTO() },
                    iverksattTidspunkt = command.iverksattTidspunkt?.format(norskTidspunktFormatter),
                    korrigering = command.erKorrigering,
                    sammenligningAvBeregninger = command.beregninger?.map {
                        sammenlign(it.first, it.second).toDTO()
                    }?.let {
                        BrevMeldekortvedtakDTO.SammenligningAvBeregningerDTO(
                            meldeperioder = it,
                            totalDifferanse = it.sumOf { periode -> periode.differanseFraForrige },
                        )
                    },
                    totaltBelop = command.totaltBeløp,
                    brevTekst = command.tekstTilVedtaksbrev?.value,
                    forhandsvisning = command.forhåndsvisning,
                ).let {
                    serialize(it)
                }
            },
            errorContext = "SakId: ${command.sakId}, saksnummer: ${command.saksnummer}, meldekortbehandlingId: ${command.meldekortbehandlingId}",
            uri = meldekortvedtakUri,
        )
    }

    override suspend fun genererStansBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                vedtak.toRevurderingStans(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    vedtaksdato = vedtaksdato,
                )
            },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}, vedtakId: ${vedtak.id}",
            uri = stansvedtakUri,
        )
    }

    override suspend fun genererStansBrevForhåndsvisning(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        stansperiode: Periode,
        saksnummer: Saksnummer,
        sakId: SakId,
        tilleggstekst: FritekstTilVedtaksbrev?,
        valgteHjemler: NonEmptySet<HjemmelForStansEllerOpphør>,
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
                    stansperiode = stansperiode,
                    saksnummer = saksnummer,
                    forhåndsvisning = true,
                    valgteHjemler = valgteHjemler,
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
        tilleggstekst: FritekstTilVedtaksbrev?,
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

    override suspend fun genererAvslagsVedtaksbrev(
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

    override suspend fun genererAvvisningsvedtak(
        saksnummer: Saksnummer,
        fnr: Fnr,
        tilleggstekst: Brevtekster,
        saksbehandlerNavIdent: String,
        vedtaksdato: LocalDate,
        forhåndsvisning: Boolean,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                BrevKlageAvvisningDTO.create(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    datoForUtsending = vedtaksdato,
                    tilleggstekst = tilleggstekst,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    saksnummer = saksnummer,
                    forhåndsvisning = forhåndsvisning,
                    fnr = fnr,
                )
            },
            errorContext = "Saksnummer: $saksnummer, Forhåndsvisning: $forhåndsvisning",
            uri = klageAvvisUri,
        )
    }

    override suspend fun genererOpphørBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                vedtak.tilBrevOmgjøringOpphørDTO(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    vedtaksdato = vedtaksdato,
                )
            },
            errorContext = "SakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}, vedtakId: ${vedtak.id}",
            uri = opphørUri,
        )
    }

    override suspend fun genererOpphørBrevForhåndsvisning(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        saksnummer: Saksnummer,
        sakId: SakId,
        tilleggstekst: FritekstTilVedtaksbrev?,
        valgteHjemler: NonEmptySet<HjemmelForStansEllerOpphør>,
        vedtaksperiode: Periode,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                genererOpphørBrev(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    vedtaksdato = vedtaksdato,
                    fnr = fnr,
                    saksbehandlerNavIdent = saksbehandlerNavIdent,
                    beslutterNavIdent = beslutterNavIdent,
                    saksnummer = saksnummer,
                    forhåndsvisning = true,
                    vedtaksperiode = vedtaksperiode,
                    valgteHjemler = valgteHjemler,
                    tilleggstekst = tilleggstekst,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = opphørUri,
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
