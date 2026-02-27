package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling

fun MeldekortBehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return if (beregning == null) Unit.right() else simulering.validerKanIverksetteUtbetaling()
}

fun Rammebehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    val simulering = this.utbetaling?.simulering
    val kontrollSimulering = this.utbetalingskontroll?.simulering

    if (!simulering.erLik(kontrollSimulering)) {
        return KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer.left()
    }

    // Hvis beregnet utbetaling er null (og kontrollen også var null), er alt ok
    if (this.utbetaling == null) {
        return Unit.right()
    }

    if (simulering == null) {
        return KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
    }

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

enum class KanIkkeIverksetteUtbetaling {
    SimuleringMangler,
    FeilutbetalingStøttesIkke,
    JusteringStøttesIkke,
    KontrollSimuleringHarEndringer,
}
