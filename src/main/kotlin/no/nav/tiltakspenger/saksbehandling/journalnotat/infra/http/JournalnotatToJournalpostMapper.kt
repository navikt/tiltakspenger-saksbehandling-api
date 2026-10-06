package no.nav.tiltakspenger.saksbehandling.journalnotat.infra.http

import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.ArkivPDF
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.OriginalJson
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalnotat
import no.nav.tiltakspenger.saksbehandling.journalnotat.infra.JOURNALNOTAT_TITTEL

/**
 * Notatet journalføres som NOTAT, som ikke distribueres og ikke vises for bruker på nav.no.
 * Dokarkiv krever at verken kanal eller avsenderMottaker settes for notater.
 * eksternReferanseId er dedup-nøkkelen i dokarkiv, og må skille seg fra vedtaksbrevet, som bruker vedtakId alene.
 */
fun Journalnotat.tilJournalpostRequest(
    pdfOgJson: PdfOgJson,
): String {
    return DokarkivRequest(
        tittel = JOURNALNOTAT_TITTEL,
        journalpostType = DokarkivRequest.JournalPostType.NOTAT,
        kanal = null,
        avsenderMottaker = null,
        bruker = DokarkivRequest.Bruker(fnr.verdi),
        sak = DokarkivRequest.DokarkivSak.Fagsak(saksnummer.verdi),
        dokumenter = listOf(
            DokarkivRequest.JournalpostDokument(
                tittel = JOURNALNOTAT_TITTEL,
                brevkode = "NOTAT-TILTAKSPENGER",
                dokumentvarianter = listOf(
                    ArkivPDF(
                        fysiskDokument = pdfOgJson.pdfAsBase64(),
                        tittel = JOURNALNOTAT_TITTEL,
                    ),
                    OriginalJson(
                        fysiskDokument = pdfOgJson.jsonAsBase64(),
                        tittel = JOURNALNOTAT_TITTEL,
                    ),
                ),
            ),
        ),
        eksternReferanseId = "$vedtakId-notat",
    ).let { objectMapper.writeValueAsString(it) }
}
