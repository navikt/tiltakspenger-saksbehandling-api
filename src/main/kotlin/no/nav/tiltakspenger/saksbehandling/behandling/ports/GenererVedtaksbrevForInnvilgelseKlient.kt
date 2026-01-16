package no.nav.tiltakspenger.saksbehandling.behandling.ports

import arrow.core.Either
import arrow.core.NonEmptyList
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
        tilleggstekst: FritekstTilVedtaksbrev?,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        innvilgelsesperioder: NonEmptyList<Periode>,
        saksnummer: Saksnummer,
        sakId: SakId,
        barnetilleggsPerioder: Periodisering<AntallBarn>?,
        antallDagerTekst: String?,
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
        innvilgelsesperioder: NonEmptyList<Periode>,
        tilleggstekst: FritekstTilVedtaksbrev?,
        barnetillegg: Periodisering<AntallBarn>?,
        antallDagerTekst: String?,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
