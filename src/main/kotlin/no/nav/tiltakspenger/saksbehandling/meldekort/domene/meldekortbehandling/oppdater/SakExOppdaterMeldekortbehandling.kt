package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock

suspend fun Sak.oppdaterMeldekort(
    kommando: OppdaterMeldekortbehandlingKommando,
    simuler: (suspend (Meldekortbehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>),
    clock: Clock,
): Either<KanIkkeOppdatereMeldekortbehandling, Triple<Sak, MeldekortUnderBehandling, SimuleringMedMetadata?>> {
    return this.meldekortbehandlinger.oppdaterMeldekort(
        kommando = kommando,
        beregn = { meldeperiode ->
            this.beregnMeldekort(
                meldekortIdSomBeregnes = kommando.meldekortId,
                meldeperioderSomBeregnes = kommando.dager.tilMeldekortDager(meldeperiode),
            )
        },
        simuler = simuler,
        clock = clock,
    ).map { Triple(this.oppdaterMeldekortbehandlinger(it.first), it.second, it.third) }
}
