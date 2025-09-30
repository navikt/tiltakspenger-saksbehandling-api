package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling

fun BehandlingUtbetaling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling()
}

fun MeldekortBehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return simulering.validerKanIverksetteUtbetaling()
}

private fun Simulering?.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> =
    if (!Configuration.isDev()) {
        when (this) {
            is Simulering.Endring -> {
                if (totalFeilutbetaling != 0 || totalMotpostering != 0) {
                    KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke.left()
                } else if (harJustering) {
                    KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.left()
                } else {
                    Unit.right()
                }
            }

            Simulering.IngenEndring -> Unit.right()
            null -> KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
        }
    } else {
        Unit.right()
    }

enum class KanIkkeIverksetteUtbetaling {
    SimuleringMangler,
    FeilutbetalingStøttesIkke,
    JusteringStøttesIkke,
}
