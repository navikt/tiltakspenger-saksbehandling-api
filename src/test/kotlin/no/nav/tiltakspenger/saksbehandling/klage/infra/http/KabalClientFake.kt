package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.FeilVedOversendelseTilKabal
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import java.time.Clock

class KabalClientFake(
    val clock: Clock,
) : KabalClient {
    override suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<FeilVedOversendelseTilKabal, OversendtKlageTilKabalMetadata> = OversendtKlageTilKabalMetadata(
        request = "{}",
        response = "",
        oversendtTidspunkt = nå(clock),
    ).right()
}
