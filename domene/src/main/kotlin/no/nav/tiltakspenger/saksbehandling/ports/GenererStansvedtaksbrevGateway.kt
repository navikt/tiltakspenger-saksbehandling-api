package no.nav.tiltakspenger.saksbehandling.ports

import arrow.core.Either
import no.nav.tiltakspenger.felles.KunneIkkeGenererePdf
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.Navn
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import java.time.LocalDate

interface GenererStansvedtaksbrevGateway {
    suspend fun genererStansvedtak(
        vedtak: Rammevedtak,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
