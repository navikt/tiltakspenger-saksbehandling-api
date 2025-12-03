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
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.periodisering.norskTidspunktFormatter
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlign
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
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
    private val meldekortvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/utbetalingsvedtak")
    private val stansvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/stansvedtak")
    private val revurderingInnvilgelseUri = URI.create("$baseUrl/api/v1/genpdf/tpts/revurderingInnvilgelse")

    override suspend fun genererInnvilgelsesvedtaksbrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                vedtak.tilInnvilgetSøknadsbrev(hentBrukersNavn, hentSaksbehandlersNavn, vedtaksdato)
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
                when (vedtak.behandling) {
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
            uri = when (vedtak.behandling) {
                is Revurdering -> revurderingInnvilgelseUri
                is Søknadsbehandling -> vedtakInnvilgelseUri
            },
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
        barnetilleggsPerioder: SammenhengendePeriodisering<AntallBarn>?,
        antallDagerTekst: String?,
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
                    antallDagerTekst = antallDagerTekst,
                )
            },
            errorContext = "SakId: $sakId, saksnummer: $saksnummer",
            uri = vedtakInnvilgelseUri,
        )
    }

    override suspend fun genererInnvilgetRevurderingBrev(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        saksnummer: Saksnummer,
        sakId: SakId,
        forhåndsvisning: Boolean,
        innvilgelsesperiode: Periode,
        tilleggstekst: FritekstTilVedtaksbrev,
        barnetillegg: SammenhengendePeriodisering<AntallBarn>?,
        antallDagerTekst: String?,
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
                    innvilgelsesperiode = innvilgelsesperiode,
                    tilleggstekst = tilleggstekst,
                    barnetilleggsPerioder = barnetillegg,
                    vedtaksdato = vedtaksdato,
                    antallDagerTekst = antallDagerTekst,
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
                    meldekortPeriode = BrevMeldekortvedtakDTO.PeriodeDTO(
                        fom = command.beregningsperiode.fraOgMed.format(norskDatoFormatter),
                        tom = command.beregningsperiode.tilOgMed.format(norskDatoFormatter),
                    ),
                    tiltak = command.tiltaksdeltakelser.map { it.toTiltakDTO() },
                    iverksattTidspunkt = command.iverksattTidspunkt?.format(norskTidspunktFormatter),
                    korrigering = command.erKorrigering,
                    sammenligningAvBeregninger = command.beregninger.map {
                        sammenlign(it.first, it.second).toDTO()
                    }.let {
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

    override suspend fun genererStansvedtak(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        stansFraFørsteDagSomGirRett: Boolean,
        stansTilSisteDagSomGirRett: Boolean,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            jsonPayload = {
                vedtak.toRevurderingStans(
                    hentBrukersNavn = hentBrukersNavn,
                    hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                    vedtaksdato = vedtaksdato,
                    stansFraFørsteDagSomGirRett = stansFraFørsteDagSomGirRett,
                    stansTilSisteDagSomGirRett = stansTilSisteDagSomGirRett,
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
        valgteHjemler: List<ValgtHjemmelForStans>,
        stansFraFørsteDagSomGirRett: Boolean,
        stansTilSisteDagSomGirRett: Boolean,
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
                    stansperiode = virkningsperiode,
                    saksnummer = saksnummer,
                    forhåndsvisning = forhåndsvisning,
                    valgteHjemler = valgteHjemler,
                    tilleggstekst = tilleggstekst,
                    stansFraFørsteDagSomGirRett = stansFraFørsteDagSomGirRett,
                    stansTilSisteDagSomGirRett = stansTilSisteDagSomGirRett,
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
