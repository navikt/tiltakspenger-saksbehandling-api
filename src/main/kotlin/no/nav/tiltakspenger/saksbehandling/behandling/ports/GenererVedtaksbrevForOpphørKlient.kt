package no.nav.tiltakspenger.saksbehandling.behandling.ports

import arrow.core.Either
import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

interface GenererVedtaksbrevForOpphørKlient {
    suspend fun genererOpphørBrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        harOpphørtBarnetillegg: Boolean,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererOpphørBrevForhåndsvisning(
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
        valgteHjemler: NonEmptySet<HjemmelForStansEllerOpphør>,
        vedtaksperiode: Periode,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
