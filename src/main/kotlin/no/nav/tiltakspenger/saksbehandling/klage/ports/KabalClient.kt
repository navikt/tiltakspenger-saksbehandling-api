package no.nav.tiltakspenger.saksbehandling.klage.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.FeilVedOversendelseTilKabal
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata
import java.time.LocalDateTime

interface KabalClient {
    suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<FeilVedOversendelseTilKabal, OversendtKlageTilKabalMetadata>
}
