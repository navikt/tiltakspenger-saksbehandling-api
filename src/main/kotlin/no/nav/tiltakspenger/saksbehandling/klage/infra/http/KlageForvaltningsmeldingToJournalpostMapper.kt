package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.ArkivPDF
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivRequest.JournalpostDokument.DokumentVariant.OriginalJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

/**
 * Klagebehandlingens id brukes allerede som eksternReferanseId for innstillingsbrevet, og dokarkiv avviser duplikater.
 * Forvaltningsmeldingen får derfor en egen referanse avledet av klagebehandlingens id.
 */
fun Klagebehandling.forvaltningsmeldingEksternReferanseId(): String = "$id-forvaltningsmelding"

fun Klagebehandling.toForvaltningsmeldingJournalpostRequest(
    pdfOgJson: PdfOgJson,
): String {
    val tittel = "Informasjon om saksbehandlingstid for klage på vedtak om tiltakspenger"
    return DokarkivRequest(
        tittel = tittel,
        journalpostType = DokarkivRequest.JournalPostType.UTGAAENDE,
        // Når vi distribuerer en utgående journalpost skal kanal være null. https://nav-it.slack.com/archives/C6W9E5GPJ/p1736327853663619
        kanal = null,
        avsenderMottaker = DokarkivRequest.AvsenderMottaker(this.fnr.verdi),
        bruker = DokarkivRequest.Bruker(this.fnr.verdi),
        sak = DokarkivRequest.DokarkivSak.Fagsak(this.saksnummer.toString()),
        dokumenter = listOf(
            DokarkivRequest.JournalpostDokument(
                tittel = tittel,
                brevkode = "KLAGE-FORVALTNINGSMELDING-TILTAKSPENGER",
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
        eksternReferanseId = forvaltningsmeldingEksternReferanseId(),
    ).let { objectMapper.writeValueAsString(it) }
}
