package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata

suspend fun Sak.oppdaterMeldekort(
    kommando: OppdaterMeldekortKommando,
    simuler: (suspend (MeldekortBehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>),
): Either<KanIkkeOppdatereMeldekort, Triple<Sak, MeldekortUnderBehandling, SimuleringMedMetadata?>> {
    return this.meldekortBehandlinger.oppdaterMeldekort(
        kommando = kommando,
        beregn = { meldeperiode ->
            this.beregn(
                meldekortIdSomBeregnes = kommando.meldekortId,
                meldeperiodeSomBeregnes = kommando.dager.tilMeldekortDager(meldeperiode),
            )
        },
        simuler = simuler,
    ).map { Triple(this.oppdaterMeldekortbehandlinger(it.first), it.second, it.third) }
}
