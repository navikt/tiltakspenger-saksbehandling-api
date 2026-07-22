package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.flatMap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
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
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.ports.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * Har ansvar for å konvertere domene til JSON som sendes til https://github.com/navikt/tiltakspenger-pdfgen for å generere PDF.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-pdfgen og https://github.com/navikt/tiltakspenger-pdfgenrs
 * Dokumentasjon: README-ene i kildekode-repoene
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * pdfgen er en intern tjeneste uten autentisering, derfor [KlientAuth.Ingen].
 *
 * Klienten logger ikke feil selv: den bærer HTTP-konteksten videre via [KunneIkkeGenererePdf], og feillogging gjøres én gang i kallende service/jobb via [no.nav.tiltakspenger.libs.httpklient.loggFeil].
 * Unntaket er en midlertidig info-linje i [runParallel] som sammenligner responstiden til pdfgen og pdfgenrs; den fjernes sammen med pdfgenrs-verifiseringen.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class PdfgenHttpClient(
    baseUrl: String,
    basePdfgenrsUrl: String,
    private val isLocalOrDev: Boolean,
    clock: Clock,
    connectTimeout: Duration = 1.seconds,
    timeout: Duration = 20.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : GenererVedtaksbrevForInnvilgelseKlient,
    GenererVedtaksbrevForMeldekortKlient,
    GenererVedtaksbrevForStansKlient,
    GenererVedtaksbrevForAvslagKlient,
    GenererVedtaksbrevForOpphørKlient,
    GenererKlagebrevKlient {

    private val log = KotlinLogging.logger {}

    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.Ingen,
        ),
        transport = transport,
    )

    private val vedtakInnvilgelseUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakInnvilgelse")
    private val pdfgenrsVedtakInnvilgelseUri = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/vedtakInnvilgelse")
    private val vedtakAvslagUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakAvslag")
    private val pdfgenrsVedtakAvslagUri = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/vedtakAvslag")
    private val meldekortvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/utbetalingsvedtak")
    private val meldekortvedtakRsUri = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/utbetalingsvedtak")
    private val meldekortvedtakV2Uri = URI.create("$baseUrl/api/v1/genpdf/tpts/meldekortvedtak")
    private val meldekortvedtakV2RsUri = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/meldekortvedtak")

    private val stansvedtakUri = URI.create("$baseUrl/api/v1/genpdf/tpts/stansvedtak")
    private val pdfgenrsStansvedtakUri = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/stansvedtak")
    private val opphørUri = URI.create("$baseUrl/api/v1/genpdf/tpts/vedtakOpphør")
    private val pdfgenrsOpphørUri = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/vedtakOpphør")
    private val revurderingInnvilgelseUri = URI.create("$baseUrl/api/v1/genpdf/tpts/revurderingInnvilgelse")
    private val pdfgenrsRevurderingInnvilgelseUri =
        URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/revurderingInnvilgelse")
    private val klageAvvisUri = URI.create("$baseUrl/api/v1/genpdf/tpts/klageAvvis")
    private val pdfgenrsKlageAvvisUri = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/klageAvvis")
    private val klageInnstillingUrl = URI.create("$baseUrl/api/v1/genpdf/tpts/klageInnstilling")
    private val pdfgenrsKlageInnstillingUrl = URI.create("$basePdfgenrsUrl/api/v1/genpdf/tpts/klageInnstilling")

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererInnvilgetVedtakBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        tilleggstekst: FritekstTilVedtaksbrev?,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
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
        }
        val uri = when (vedtak.rammebehandling) {
            is Revurdering -> revurderingInnvilgelseUri
            is Søknadsbehandling -> vedtakInnvilgelseUri
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = uri,
                pdfgenrsUri = when (vedtak.rammebehandling) {
                    is Revurdering -> pdfgenrsRevurderingInnvilgelseUri
                    is Søknadsbehandling -> pdfgenrsVedtakInnvilgelseUri
                },
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = uri)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
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
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
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
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = vedtakInnvilgelseUri,
                pdfgenrsUri = pdfgenrsVedtakInnvilgelseUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = vedtakInnvilgelseUri)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
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
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
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
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = revurderingInnvilgelseUri,
                pdfgenrsUri = pdfgenrsRevurderingInnvilgelseUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = revurderingInnvilgelseUri)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererMeldekortvedtakBrev(
        meldekortvedtak: Meldekortvedtak,
        tiltaksdeltakelser: Tiltaksdeltakelser,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
            meldekortvedtak.toJsonRequest(
                hentSaksbehandlersNavn,
                tiltaksdeltakelser,
                sammenligning,
            )
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = meldekortvedtakUri,
                pdfgenrsUri = meldekortvedtakRsUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = meldekortvedtakUri)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererMeldekortvedtakBrev(
        kommando: GenererMeldekortvedtakBrevKommando,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend { kommando.tilJsonRequest(hentSaksbehandlersNavn) }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = meldekortvedtakUri,
                pdfgenrsUri = meldekortvedtakRsUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = meldekortvedtakUri)
                .map { it to null }
        }
    }

    override suspend fun genererMeldekortvedtakBrevV2(
        meldekortvedtak: Meldekortvedtak,
        tiltaksdeltakelser: Tiltaksdeltakelser,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
            meldekortvedtak.toJsonRequestV2(
                hentSaksbehandlersNavn,
                tiltaksdeltakelser,
                sammenligning,
            )
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = meldekortvedtakV2Uri,
                pdfgenrsUri = meldekortvedtakV2RsUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = meldekortvedtakV2Uri)
                .map { it to null }
        }
    }

    override suspend fun genererMeldekortvedtakBrevV2(
        kommando: GenererMeldekortvedtakBrevKommando,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend { kommando.tilJsonRequestV2(hentSaksbehandlersNavn) }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = meldekortvedtakV2Uri,
                pdfgenrsUri = meldekortvedtakV2RsUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = meldekortvedtakV2Uri)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererStansBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        harStansetBarnetillegg: Boolean,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
            vedtak.toRevurderingStans(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                vedtaksdato = vedtaksdato,
                harStansetBarnetillegg = harStansetBarnetillegg,
            )
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = stansvedtakUri,
                pdfgenrsUri = pdfgenrsStansvedtakUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = stansvedtakUri)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererStansBrevForhåndsvisning(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        harStansetBarnetillegg: Boolean,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        stansperiode: Periode,
        saksnummer: Saksnummer,
        sakId: SakId,
        tilleggstekst: FritekstTilVedtaksbrev?,
        valgteHjemler: NonEmptySet<HjemmelForStans>,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
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
                harStansetBarnetillegg = harStansetBarnetillegg,
            )
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = stansvedtakUri,
                pdfgenrsUri = pdfgenrsStansvedtakUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = stansvedtakUri)
                .map { it to null }
        }
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
        val jsonPayload = suspend {
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
        }

        return pdfgenRequest(jsonPayload = jsonPayload, uri = pdfgenrsVedtakAvslagUri)
    }

    override suspend fun genererAvslagsVedtaksbrev(
        vedtak: Rammevedtak,
        datoForUtsending: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        val jsonPayload = suspend {
            vedtak.genererAvslagSøknadsbrev(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                datoForUtsending = datoForUtsending,
            )
        }

        return pdfgenRequest(jsonPayload = jsonPayload, uri = pdfgenrsVedtakAvslagUri)
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererAvvisningsvedtak(
        saksnummer: Saksnummer,
        fnr: Fnr,
        tilleggstekst: Brevtekster,
        saksbehandlerNavIdent: String,
        vedtaksdato: LocalDate,
        forhåndsvisning: Boolean,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
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
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = klageAvvisUri,
                pdfgenrsUri = pdfgenrsKlageAvvisUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = klageAvvisUri)
                .map { it to null }
        }
    }

    override suspend fun genererInnstillingsbrev(
        saksnummer: Saksnummer,
        fnr: Fnr,
        tilleggstekst: Brevtekster,
        saksbehandlerNavIdent: String,
        forhåndsvisning: Boolean,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        innsendingsdato: LocalDate,
        clock: Clock,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
            BrevKlageInnstillingDTO.create(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                datoForUtsending = LocalDate.now(clock),
                tilleggstekst = tilleggstekst,
                saksbehandlerNavIdent = saksbehandlerNavIdent,
                saksnummer = saksnummer,
                forhåndsvisning = forhåndsvisning,
                fnr = fnr,
                vedtaksdato = vedtaksdato,
                innsendingsdato = innsendingsdato,
            )
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = klageInnstillingUrl,
                pdfgenrsUri = pdfgenrsKlageInnstillingUrl,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = klageInnstillingUrl)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererOpphørBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        harOpphørtBarnetillegg: Boolean,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
            vedtak.tilBrevOmgjøringOpphørDTO(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                harOpphørtBarnetillegg = harOpphørtBarnetillegg,
                vedtaksdato = vedtaksdato,
            )
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = opphørUri,
                pdfgenrsUri = pdfgenrsOpphørUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = opphørUri)
                .map { it to null }
        }
    }

    /*
        TODO - pdfgenrs: skift tilbake til Either<KunneIkkeGenererePdf, PdfOgJson> når det er verifisert at PDF pdfgenrs er ok
     */
    override suspend fun genererOpphørBrevForhåndsvisning(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        harOpphørtBarnetillegg: Boolean,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        saksnummer: Saksnummer,
        sakId: SakId,
        tilleggstekst: FritekstTilVedtaksbrev?,
        valgteHjemler: NonEmptySet<HjemmelForOpphør>,
        vedtaksperiode: Periode,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        val jsonPayload = suspend {
            genererOpphørBrev(
                hentBrukersNavn = hentBrukersNavn,
                hentSaksbehandlersNavn = hentSaksbehandlersNavn,
                harOpphørtBarnetillegg = harOpphørtBarnetillegg,
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
        }

        return if (isLocalOrDev) {
            runParallel(
                jsonPayload = jsonPayload,
                pdfgenUri = opphørUri,
                pdfgenrsUri = pdfgenrsOpphørUri,
            )
        } else {
            pdfgenRequest(jsonPayload = jsonPayload, uri = opphørUri)
                .map { it to null }
        }
    }

    /**
     * Payloaden bygges før HTTP-kallet, og feil derfra (f.eks. navneoppslag som kaster) propagerer som exceptions akkurat som før migreringen.
     * Payloaden serialiseres av kallerne og sendes verbatim ([SerialisertJson]) fordi nøyaktig samme JSON persisteres sammen med PDF-en ([PdfOgJson]).
     */
    private suspend fun pdfgenRequest(
        jsonPayload: suspend () -> String,
        uri: URI,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        val payload = jsonPayload()
        return httpKlient.postJsonMotPdf(
            uri = uri,
            body = SerialisertJson(payload),
            godta = Statusregel.Eksakt(200),
        ).map { PdfOgJson(PdfA(it.body), payload) }
            .mapLeft { KunneIkkeGenererePdf(it) }
    }

    /**
     * Kaller pdfgen og pdfgenrs i parallell (kun local/dev) slik at PDF-ene kan sammenlignes manuelt.
     * Feiler én av dem, feiler hele kallet; feiler begge, er det pdfgen-feilen som propageres til kallerens feillogging.
     */
    private suspend fun runParallel(
        jsonPayload: suspend () -> String,
        pdfgenUri: URI,
        pdfgenrsUri: URI,
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>> {
        return coroutineScope {
            val pdfgenDeferred = async {
                measureTimedValue { pdfgenRequest(jsonPayload, pdfgenUri) }
            }
            val pdfgenrsDeferred = async {
                measureTimedValue { pdfgenRequest(jsonPayload, pdfgenrsUri) }
            }

            val (pdfgenResult, pdfgenDuration) = pdfgenDeferred.await()
            val (pdfgenrsResult, pdfgenrsDuration) = pdfgenrsDeferred.await()

            log.info { "pdfgen brukte $pdfgenDuration, pdfgenrs brukte $pdfgenrsDuration" }

            pdfgenResult.flatMap { pdfgen ->
                pdfgenrsResult.map { pdfgenrs ->
                    Pair(pdfgen, pdfgenrs)
                }
            }
        }
    }
}
