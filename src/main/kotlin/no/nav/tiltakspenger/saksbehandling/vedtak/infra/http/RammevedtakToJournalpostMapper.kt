package no.nav.tiltakspenger.saksbehandling.vedtak.infra.http

import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.ArkivPDF
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.OriginalJson
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/** Denne ligger nærmere vedtak enn journalføring strukturmessig */
internal fun Rammevedtak.utgåendeJournalpostRequest(
    pdfOgJson: PdfOgJson,
): String {
    return toJournalpostRequest(
        pdfOgJson = pdfOgJson,
        journalPostType = DokarkivRequest.JournalPostType.UTGAAENDE,
        // Når vi distribuerer en utgående journalpost skal kanal være null. https://nav-it.slack.com/archives/C6W9E5GPJ/p1736327853663619 Hvis vi skal distribuere den selv, må vi se på dette igjen.
        kanal = null,
        avsenderMottaker = DokarkivRequest.AvsenderMottaker(this.fnr.verdi),
    )
}

private fun Rammevedtak.toJournalpostRequest(
    pdfOgJson: PdfOgJson,
    journalPostType: DokarkivRequest.JournalPostType,
    kanal: String?,
    avsenderMottaker: DokarkivRequest.AvsenderMottaker?,
): String {
    val tittel = "Vedtak om tiltakspenger"
    return DokarkivRequest(
        tittel = tittel,
        journalpostType = journalPostType,
        kanal = kanal,
        avsenderMottaker = avsenderMottaker,
        bruker = DokarkivRequest.Bruker(this.fnr.verdi),
        sak = DokarkivRequest.DokarkivSak.Fagsak(this.saksnummer.toString()),
        dokumenter = listOf(
            DokarkivRequest.JournalpostDokument(
                tittel = tittel,
                // TODO jah:brevkode bør være bakt inn i PdfOgJson og settes samtidig som vi mottar PDFen.
                brevkode = "MELDEKORT-TILTAKSPENGER",
                dokumentvarianter = listOf(
                    ArkivPDF(
                        fysiskDokument = pdfOgJson.pdfAsBase64(),
                        tittel = tittel,
                    ),
                    OriginalJson(
                        fysiskDokument = pdfOgJson.jsonAsBase64(),
                        tittel = tittel,
                    ),
                ),
            ),
        ),
        eksternReferanseId = this.id.toString(),
    ).let { objectMapper.writeValueAsString(it) }
}
