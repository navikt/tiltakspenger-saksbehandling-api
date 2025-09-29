package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling

fun BehandlingUtbetaling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling()
}

private fun Simulering?.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return when (this) {
        is Simulering.Endring -> {
            if (this.totalFeilutbetaling > 0) {
                KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke.left()
            } else if (this.totalJustering > 0) {
                KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.left()
            } else {
                Unit.right()
            }
        }

        Simulering.IngenEndring -> Unit.right()
        null -> KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
    }
}

sealed interface KanIkkeIverksetteUtbetaling {
    data object SimuleringMangler : KanIkkeIverksetteUtbetaling
    data object FeilutbetalingStøttesIkke : KanIkkeIverksetteUtbetaling
    data object JusteringStøttesIkke : KanIkkeIverksetteUtbetaling
}
