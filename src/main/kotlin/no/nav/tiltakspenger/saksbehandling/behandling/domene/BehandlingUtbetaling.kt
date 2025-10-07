package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregninger
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning

/**
 * @param simulering Vi gjør bare en best-effort på å simulere.
 */
data class BehandlingUtbetaling(
    val beregning: Beregning,
    val navkontor: Navkontor,
    val simulering: Simulering?,
) {
    fun toSimulertBeregning(beregninger: MeldeperiodeBeregninger): SimulertBeregning {
        return SimulertBeregning.create(
            beregning = beregning,
            eksisterendeBeregninger = beregninger,
            simulering = simulering,
        )
    }

    fun oppdaterSimulering(nySimulering: Simulering?) = copy(simulering = nySimulering)
}
