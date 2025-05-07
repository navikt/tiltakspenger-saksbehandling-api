package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.oppdaterMeldekort(
    kommando: OppdaterMeldekortKommando,
): Either<KanIkkeOppdatereMeldekort, Pair<Sak, MeldekortUnderBehandling>> {
    return this.meldekortBehandlinger.oppdaterMeldekort(
        kommando = kommando,
        beregn = { meldeperiode ->
            this.beregn(
                meldekortIdSomBeregnes = kommando.meldekortId,
                meldeperiodeSomBeregnes = kommando.dager.tilMeldekortDager(meldeperiode),
            )
        },
    )
        .map {
            Pair(this.oppdaterMeldekortbehandling(it.second), it.second)
        }
}
