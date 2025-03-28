package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

class GenererFakeVedtaksbrevGateway :
    GenererInnvilgelsesvedtaksbrevGateway,
    GenererStansvedtaksbrevGateway {
    private val response by lazy { PdfOgJson(PdfA("pdf".toByteArray()), "json").right() }
    override suspend fun genererInnvilgelsesvedtaksbrev(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return response
    }

    override suspend fun genererInnvilgelsesvedtaksbrevMedTilleggstekst(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        tilleggstekst: FritekstTilVedtaksbrev?,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return response
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
        return response
    }

    override suspend fun genererStansvedtak(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return response
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
        return response
    }
}
