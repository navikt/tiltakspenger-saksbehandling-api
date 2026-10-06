package no.nav.tiltakspenger.saksbehandling.journalnotat.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.tilJournalnotatDokumentJson
import no.nav.tiltakspenger.saksbehandling.journalnotat.GenererJournalnotatKlient
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalnotat
import no.nav.tiltakspenger.saksbehandling.person.Navn

/**
 * Bygger den ekte pdfgenrs-payloaden, men returnerer en dummy-PDF.
 */
class GenererFakeJournalnotatKlient : GenererJournalnotatKlient {

    override suspend fun genererJournalnotat(
        journalnotat: Journalnotat,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        val json = journalnotat.tilJournalnotatDokumentJson(
            hentBrukersNavn = hentBrukersNavn,
            hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        )
        return PdfOgJson(PdfA("pdf".toByteArray()), json).right()
    }
}
