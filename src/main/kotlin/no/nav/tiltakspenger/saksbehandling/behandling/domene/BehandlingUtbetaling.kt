package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger

/**
 * @param simulering Vi gjør bare en best-effort på å simulere.
 */
data class BehandlingUtbetaling(
    val beregning: BehandlingBeregning,
    val navkontor: Navkontor,
    val simulering: Simulering?,
) {
    fun toSimulertBeregning(tidligereUtbetalinger: Utbetalinger): SimulertBeregning {
        return SimulertBeregning.create(
            beregning = beregning,
            tidligereUtbetalinger = tidligereUtbetalinger,
            simulering = simulering,
        )
    }

    fun oppdaterSimulering(nySimulering: Simulering?) = copy(simulering = nySimulering)
}
