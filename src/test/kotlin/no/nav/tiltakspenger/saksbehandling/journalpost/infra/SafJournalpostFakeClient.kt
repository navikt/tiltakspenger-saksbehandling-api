package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.HentDokumentCommand
import java.time.Clock
import java.time.LocalDateTime

class SafJournalpostFakeClient(
    private val clock: Clock,
) : SafJournalpostClient {
    val data = arrow.atomic.Atomic(mutableMapOf<JournalpostId, Fnr>())

    override suspend fun hentJournalpost(
        journalpostId: JournalpostId,
    ): Journalpost? {
        // Bare for å kunne trigge de forskjellige tilstandene ved lokal kjøring
        // fnr her er det som brukes for den ene søknaden som finnes lokalt
        if (journalpostId.toString() == "12345") {
            return Journalpost(
                avsenderMottaker = AvsenderMottaker(
                    id = "12345678911",
                    type = "FNR",
                ),
                datoOpprettet = LocalDateTime.now(clock).toString(),
                bruker = Bruker(
                    id = "12345678911",
                    type = "FNR",
                ),
            )
        }

        // Journalpost finnes, men på et annet fnr
        if (journalpostId.toString() == "123456") {
            val fnr = Fnr.random().verdi
            return Journalpost(
                avsenderMottaker = AvsenderMottaker(
                    id = fnr,
                    type = "FNR",
                ),
                datoOpprettet = LocalDateTime.now(clock).toString(),
                bruker = Bruker(
                    id = fnr,
                    type = "FNR",
                ),
            )
        }

        return data.get()[journalpostId]?.let {
            Journalpost(
                avsenderMottaker = AvsenderMottaker(
                    id = it.verdi,
                    type = "FNR",
                ),
                datoOpprettet = LocalDateTime.now(clock).toString(),
                bruker = Bruker(
                    id = it.verdi,
                    type = "FNR",
                ),
            )
        }
    }

    private val pdf =
        """%PDF-1.0
                1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj
                xref
                0 4
                0000000000 65535 f
                0000000010 00000 n
                0000000053 00000 n
                0000000102 00000 n
                trailer<</Size 4/Root 1 0 R>>
                startxref
                149
                %EOF
        """.trimIndent()

    override suspend fun hentDokument(command: HentDokumentCommand): PdfA {
        return PdfA(pdf.toByteArray())
    }

    fun addJournalpost(journalpostId: JournalpostId, fnr: Fnr) {
        data.get()[journalpostId] = fnr
    }
}
