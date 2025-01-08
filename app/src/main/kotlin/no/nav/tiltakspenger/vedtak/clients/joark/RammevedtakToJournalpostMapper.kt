package no.nav.tiltakspenger.vedtak.clients.joark

import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.vedtak.clients.joark.JoarkRequest.JournalpostDokument.DokumentVariant.ArkivPDF
import no.nav.tiltakspenger.vedtak.clients.joark.JoarkRequest.JournalpostDokument.DokumentVariant.OriginalJson

internal fun Rammevedtak.utgåendeJournalpostRequest(
    pdfOgJson: PdfOgJson,
): String {
    return toJournalpostRequest(
        pdfOgJson = pdfOgJson,
        journalPostType = JoarkRequest.JournalPostType.UTGAAENDE,
        // Når vi distribuerer en utgående journalpost skal kanal være null. https://nav-it.slack.com/archives/C6W9E5GPJ/p1736327853663619 Hvis vi skal distribuere den selv, må vi se på dette igjen.
        kanal = null,
        avsenderMottaker = JoarkRequest.AvsenderMottaker(this.fnr.verdi),
    )
}

/**
 * Kommentar jah: Ikke sikkert vi får bruk for notat av rammevedtak. Kan i så fall bare slettes.
 */
internal fun Rammevedtak.notatJournalpostRequest(
    pdfOgJson: PdfOgJson,
): String {
    return toJournalpostRequest(
        pdfOgJson = pdfOgJson,
        journalPostType = JoarkRequest.JournalPostType.NOTAT,
        // Fra doccen: "Kanal skal ikke settes for notater."
        kanal = null,
        // Fra doccen: "avsenderMottaker skal ikke settes for journalposttype NOTAT."
        avsenderMottaker = null,
    )
}

private fun Rammevedtak.toJournalpostRequest(
    pdfOgJson: PdfOgJson,
    journalPostType: JoarkRequest.JournalPostType,
    kanal: String?,
    avsenderMottaker: JoarkRequest.AvsenderMottaker?,
): String {
    val tittel = "Vedtak om tiltakspenger"
    return JoarkRequest(
        tittel = tittel,
        journalpostType = journalPostType,
        kanal = kanal,
        avsenderMottaker = avsenderMottaker,
        bruker = JoarkRequest.Bruker(this.fnr.verdi),
        sak = JoarkRequest.Sak.Fagsak(this.saksnummer.toString()),
        dokumenter = listOf(
            JoarkRequest.JournalpostDokument(
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
