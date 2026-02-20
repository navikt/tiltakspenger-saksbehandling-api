package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
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

    fun addJournalpost(journalpostId: JournalpostId, fnr: Fnr) {
        data.get()[journalpostId] = fnr
    }
}
