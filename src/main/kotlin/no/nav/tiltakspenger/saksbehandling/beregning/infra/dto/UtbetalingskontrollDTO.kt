package no.nav.tiltakspenger.saksbehandling.beregning.infra.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.erLik
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO
import java.time.LocalDateTime

sealed interface UtbetalingskontrollDTO {
    val harEndringer: Boolean
    val tidspunkt: LocalDateTime

    data class UtbetalingskontrollMedEndringDTO(
        override val tidspunkt: LocalDateTime,
        val simulertBeregning: SimulertBeregningDTO,
    ) : UtbetalingskontrollDTO {
        override val harEndringer: Boolean = true
    }

    // Vi bryr oss ikke om kontroll-simuleringen i frontend hvis det ikke er noen endringer
    data class UtbetalingskontrollUtenEndringDTO(
        override val tidspunkt: LocalDateTime,
    ) : UtbetalingskontrollDTO {
        override val harEndringer: Boolean = false
    }
}

fun Utbetalingskontroll.tilUtbetalingskontrollDTO(
    behandlingUtbetaling: BehandlingUtbetaling?,
    beregninger: MeldeperiodeBeregningerVedtatt,
): UtbetalingskontrollDTO {
    val kontrollsimuleringErLik = this.simulering.erLik(behandlingUtbetaling?.simulering)
    val tidspunkt = this.simulering.simuleringstidspunkt

    return when (kontrollsimuleringErLik) {
        true -> UtbetalingskontrollDTO.UtbetalingskontrollUtenEndringDTO(
            tidspunkt = tidspunkt,
        )

        false -> UtbetalingskontrollDTO.UtbetalingskontrollMedEndringDTO(
            tidspunkt = tidspunkt,
            simulertBeregning = this.toSimulertBeregning(beregninger).toSimulertBeregningDTO(),
        )
    }
}
