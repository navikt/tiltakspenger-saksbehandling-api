package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.ArkivPDF
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.OriginalJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

fun Klagebehandling.toJournalpostRequest(
    pdfOgJson: PdfOgJson,
): String {
    val tittel = "Opprettholdelse av klage på vedtak om tiltakspenger"
    return DokarkivRequest(
        tittel = tittel,
        journalpostType = DokarkivRequest.JournalPostType.UTGAAENDE,
        // Når vi distribuerer en utgående journalpost skal kanal være null. https://nav-it.slack.com/archives/C6W9E5GPJ/p1736327853663619 Hvis vi skal distribuere den selv, må vi se på dette igjen.
        kanal = null,
        avsenderMottaker = DokarkivRequest.AvsenderMottaker(this.fnr.verdi),
        bruker = DokarkivRequest.Bruker(this.fnr.verdi),
        sak = DokarkivRequest.DokarkivSak.Fagsak(this.saksnummer.toString()),
        dokumenter = listOf(
            DokarkivRequest.JournalpostDokument(
                tittel = tittel,
                brevkode = "KLAGE-OPPRETTHOLDELSE-TILTAKSPENGER",
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
