package no.nav.tiltakspenger.saksbehandling.journalnotat

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.person.Navn

interface GenererJournalnotatKlient {
    suspend fun genererJournalnotat(
        journalnotat: Journalnotat,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
