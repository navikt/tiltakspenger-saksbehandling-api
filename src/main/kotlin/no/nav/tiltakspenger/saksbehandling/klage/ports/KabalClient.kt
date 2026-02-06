package no.nav.tiltakspenger.saksbehandling.klage.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

interface KabalClient {
    suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<FeilVedOversendelseTilKabal, Unit>
}

data object FeilVedOversendelseTilKabal
