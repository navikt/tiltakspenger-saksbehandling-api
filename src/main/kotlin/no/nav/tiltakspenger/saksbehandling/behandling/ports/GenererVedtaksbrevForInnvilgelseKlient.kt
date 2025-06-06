package no.nav.tiltakspenger.saksbehandling.behandling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

interface GenererVedtaksbrevForInnvilgelseKlient {
    suspend fun genererInnvilgelsesvedtaksbrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererInnvilgelsesvedtaksbrevMedTilleggstekst(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        tilleggstekst: FritekstTilVedtaksbrev?,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererInnvilgelsesvedtaksbrevMedTilleggstekst(
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
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererInnvilgetRevurderingBrev(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        saksnummer: Saksnummer,
        sakId: SakId,
        forhåndsvisning: Boolean,
        vurderingsperiode: Periode,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
