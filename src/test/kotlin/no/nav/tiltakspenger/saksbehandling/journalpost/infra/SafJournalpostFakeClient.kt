package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.time.LocalDate

class SafJournalpostFakeClient : SafJournalpostClient {
    val data = arrow.atomic.Atomic(mutableMapOf<JournalpostId, Fnr>())

    override suspend fun hentJournalpost(journalpostId: JournalpostId): Journalpost? {
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
