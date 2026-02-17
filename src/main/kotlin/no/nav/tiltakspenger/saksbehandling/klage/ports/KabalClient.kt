package no.nav.tiltakspenger.saksbehandling.klage.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import java.time.LocalDateTime

interface KabalClient {
    suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<FeilVedOversendelseTilKabal, OversendtKlageTilKabal>
}

data object FeilVedOversendelseTilKabal

data class OversendtKlageTilKabal(
    val request: String,
    val response: String,
    val oversendtTidspunkt: LocalDateTime,
)
