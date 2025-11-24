package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.time.LocalDateTime

class SafJournalpostFakeClient : SafJournalpostClient {
    val data = arrow.atomic.Atomic(mutableMapOf<JournalpostId, Fnr>())

    override suspend fun hentJournalpost(journalpostId: JournalpostId): Journalpost? {
        // Bare for å kunne trigge de forskjellige tilstandene ved lokal kjøring
        // fnr her er det som brukes for den ene søknaden som finnes lokalt
        if (journalpostId.toString() == "123") {
            return Journalpost(
                avsenderMottaker = AvsenderMottaker(
                    id = "12345678911",
                    type = "FNR",
                ),
                datoOpprettet = LocalDateTime.now().toString(),
            )
        }

        // Journalpost finnes, men på et annet fnr
        if (journalpostId.toString() == "1234") {
            return Journalpost(
                avsenderMottaker = AvsenderMottaker(
                    id = Fnr.random().verdi,
                    type = "FNR",
                ),
                datoOpprettet = LocalDateTime.now().toString(),
            )
        }

        return data.get()[journalpostId]?.let {
            Journalpost(
                avsenderMottaker = AvsenderMottaker(
                    id = it.verdi,
                    type = "FNR",
                ),
                datoOpprettet = LocalDateTime.now().toString(),
            )
        }
    }

    fun addJournalpost(journalpostId: JournalpostId, fnr: Fnr) {
        data.get()[journalpostId] = fnr
    }
}
