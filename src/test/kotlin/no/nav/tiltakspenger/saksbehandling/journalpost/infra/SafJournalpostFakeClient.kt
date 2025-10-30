package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.time.LocalDate

class SafJournalpostFakeClient : SafJournalpostClient {
    val data = arrow.atomic.Atomic(mutableMapOf<JournalpostId, Fnr>())

    override suspend fun hentJournalpost(journalpostId: JournalpostId): Journalpost? {
        // Bare for å kunne trigge de forskjellige tilstandene ved lokal kjøring
        // fnr her er det som brukes for den ene søknaden som finnes lokalt
        if (journalpostId.toString() == "123") {
            return Journalpost(
                bruker = Bruker(
                    id = "12345678911",
                    type = BrukerIdType.FNR,
                ),
                datoOpprettet = objectMapper.writeValueAsString(LocalDate.now()),
            )
        }

        // Journalpost finnes, men på et annet fnr
        if (journalpostId.toString() == "1234") {
            return Journalpost(
                bruker = Bruker(
                    id = Fnr.random().verdi,
                    type = BrukerIdType.FNR,
                ),
                datoOpprettet = objectMapper.writeValueAsString(LocalDate.now()),
            )
        }

        return data.get()[journalpostId]?.let {
            Journalpost(
                bruker = Bruker(
                    id = it.verdi,
                    type = BrukerIdType.FNR,
                ),
                datoOpprettet = objectMapper.writeValueAsString(LocalDate.now()),
            )
        }
    }

    fun addJournalpost(journalpostId: JournalpostId, fnr: Fnr) {
        data.get()[journalpostId] = fnr
    }
}
