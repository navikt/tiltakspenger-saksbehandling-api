package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.BarnSomFyller16
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

interface GenererVedtaksbrevForInnvilgelseKlient {

    /**
     * Forlengelser har eget brev, se [genererForlengelseBrev].
     */
    suspend fun genererInnvilgetVedtakBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        tilleggstekst: FritekstTilVedtaksbrev?,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererInnvilgetSøknadBrevForhåndsvisning(
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
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererInnvilgetRevurderingBrevForhåndsvisning(
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
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererForlengelseBrev(
        vedtak: Rammevedtak,
        barnSomFyller16: BarnSomFyller16?,
        vedtaksdato: LocalDate,
        tilleggstekst: FritekstTilVedtaksbrev?,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererForlengelseBrevForhåndsvisning(
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
        barnSomFyller16: BarnSomFyller16?,
        tilleggstekst: FritekstTilVedtaksbrev?,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
