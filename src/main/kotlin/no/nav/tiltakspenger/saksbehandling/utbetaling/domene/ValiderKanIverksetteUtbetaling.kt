package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling

fun BehandlingUtbetaling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling()
}

fun MeldekortBehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling()
}

private fun Simulering?.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
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
        if (!meldeperiode.harJustering) {
            return@any false
        }

        /**
         *  Dersom meldeperioden går over to måneder, må vi sjekke dagene på hver side av månedsskiftet separat
         *  Dette ettersom oppdrag kun justerer innenfor samme kalendermåned. På tvers av måneder blir det
         *  feilutbetaling + etterbetaling for hver måned istedenfor justering
         */
        return meldeperiode.simuleringsdager
            .groupBy { it.dato.month }.values
            .any { dagerForMåned ->
                dagerForMåned.sumOf { it.totalJustering } != 0
            }
    }
}

enum class KanIkkeIverksetteUtbetaling {
    SimuleringMangler,
    FeilutbetalingStøttesIkke,
    JusteringStøttesIkke,
}
