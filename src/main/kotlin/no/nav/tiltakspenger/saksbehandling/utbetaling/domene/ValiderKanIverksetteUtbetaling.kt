package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling

fun BehandlingUtbetaling.validerKanIverksetteUtbetaling(forrigeBeregnedeUtbetaling: BehandlingUtbetaling?): Either<KanIkkeIverksetteUtbetaling, Unit> {
    if (forrigeBeregnedeUtbetaling?.simulering.harEndringer(this.simulering)) {
        return KanIkkeIverksetteUtbetaling.SimuleringHarEndringer.left()
    }

    return simulering.validerKanIverksetteUtbetaling()
}

fun MeldekortBehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling()
}

fun Simulering?.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return when (this) {
        is Simulering.Endring -> {
            if ((totalFeilutbetaling != 0 || totalMotpostering != 0)) {
                KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke.left()
            } else if (harJusteringPåTversAvMeldeperioderEllerMåneder()) {
                KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.left()
            } else {
                Unit.right()
            }
        }

        is Simulering.IngenEndring -> Unit.right()

        null -> KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
    }
}

// Vi har ikke lov til å justere utbetalinger på tvers av meldeperioder
private fun Simulering.Endring.harJusteringPåTversAvMeldeperioderEllerMåneder(): Boolean {
    return simuleringPerMeldeperiode.any { meldeperiode ->
        /*
          Dersom meldeperioden går over to måneder, må vi sjekke dagene på hver side av månedsskiftet separat
          Dette ettersom oppdrag kun justerer innenfor samme kalendermåned. På tvers av måneder blir det
          feilutbetaling + etterbetaling for hver måned istedenfor justering
         */
        meldeperiode.harJustering && meldeperiode.simuleringsdager
            .groupBy { it.dato.month }.values
            .any { dagerForMåned ->
                dagerForMåned.sumOf { it.totalJustering } != 0
            }
    }
}

private fun Simulering?.harEndringer(ny: Simulering?): Boolean {
    if (this == null && ny == null) {
        return false
    }

    if (this is Simulering.IngenEndring && ny is Simulering.IngenEndring) {
        return false
    }

    if (this is Simulering.Endring && ny is Simulering.Endring) {
        return this.simuleringPerMeldeperiode != ny.simuleringPerMeldeperiode
    }

    return true
}

enum class KanIkkeIverksetteUtbetaling {
    SimuleringMangler,
    FeilutbetalingStøttesIkke,
    JusteringStøttesIkke,
    SimuleringHarEndringer,
}
