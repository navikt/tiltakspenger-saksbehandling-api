package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning

interface BeregningMedSimulering {
    val beregning: Beregning
    val simulering: Simulering?

    fun toSimulertBeregning(beregninger: MeldeperiodeBeregningerVedtatt): SimulertBeregning {
        return SimulertBeregning.create(
            beregning = beregning,
            eksisterendeBeregninger = beregninger,
            simulering = simulering,
        )
    }
}
