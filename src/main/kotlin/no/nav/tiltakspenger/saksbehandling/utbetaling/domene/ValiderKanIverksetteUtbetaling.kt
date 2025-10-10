package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling

private typealias SjekkOmMånederHarDag7 = (periode: Periode) -> Boolean

fun BehandlingUtbetaling.validerKanIverksetteUtbetaling(harDag7Utbetaling: SjekkOmMånederHarDag7): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling(harDag7Utbetaling)
}

fun MeldekortBehandling.validerKanIverksetteUtbetaling(harDag7Utbetaling: SjekkOmMånederHarDag7): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling(harDag7Utbetaling)
}

private fun Simulering?.validerKanIverksetteUtbetaling(harDag7Utbetaling: SjekkOmMånederHarDag7): Either<KanIkkeIverksetteUtbetaling, Unit> =
    when (this) {
        is Simulering.Endring -> {
            if ((totalFeilutbetaling != 0 || totalMotpostering != 0) && harDag7Utbetaling(totalPeriode)) {
                KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke.left()
            } else if (harJustering) {
                KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.left()
            } else {
                Unit.right()
            }
        }

        is Simulering.IngenEndring -> Unit.right()
        null -> KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
    }

enum class KanIkkeIverksetteUtbetaling {
    SimuleringMangler,
    FeilutbetalingStøttesIkke,
    JusteringStøttesIkke,
}
