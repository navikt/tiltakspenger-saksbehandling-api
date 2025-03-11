package no.nav.tiltakspenger.saksbehandling.saksbehandling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.Navn
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Rammevedtak
import java.time.LocalDate

interface GenererStansvedtaksbrevGateway {
    suspend fun genererStansvedtak(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererStansvedtak(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        stansperiode: Periode,
        saksnummer: Saksnummer,
        sakId: SakId,
        forhåndsvisning: Boolean,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
