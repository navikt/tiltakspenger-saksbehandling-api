package no.nav.tiltakspenger.saksbehandling.beregning.infra.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.erLik
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO
import java.time.LocalDateTime

sealed interface UtbetalingskontrollDTO {
    val status: UtbetalingskontrollStatus
    val tidspunkt: LocalDateTime

    data class UtbetalingskontrollMedEndringDTO(
        override val tidspunkt: LocalDateTime,
        val simulertBeregning: SimulertBeregningDTO,
    ) : UtbetalingskontrollDTO {
        override val status = UtbetalingskontrollStatus.ENDRET
    }

    // Vi bryr oss ikke om kontroll-simuleringen i frontend hvis det ikke er noen endringer
    data class UtbetalingskontrollUtenEndringDTO(
        override val tidspunkt: LocalDateTime,
    ) : UtbetalingskontrollDTO {
        override val status = UtbetalingskontrollStatus.UENDRET
    }

    // Vi bryr oss ikke om kontroll-simuleringen i frontend hvis den er utdatert
    data class UtbetalingskontrollUtdatertDTO(
        override val tidspunkt: LocalDateTime,
    ) : UtbetalingskontrollDTO {
        override val status = UtbetalingskontrollStatus.UTDATERT
    }

    enum class UtbetalingskontrollStatus {
        ENDRET,
        UENDRET,
        UTDATERT,
    }
}

fun Utbetalingskontroll.tilUtbetalingskontrollDTO(
    behandlingUtbetaling: BehandlingUtbetaling?,
    beregninger: MeldeperiodeBeregningerVedtatt,
): UtbetalingskontrollDTO {
    val behandlingSimulering = behandlingUtbetaling?.simulering

    if (behandlingSimulering != null && this.tidspunkt < behandlingSimulering.simuleringstidspunkt) {
        return UtbetalingskontrollDTO.UtbetalingskontrollUtdatertDTO(
            tidspunkt = this.tidspunkt,
        )
    }

    return when (this.simulering.erLik(behandlingSimulering)) {
        true -> UtbetalingskontrollDTO.UtbetalingskontrollUtenEndringDTO(
            tidspunkt = this.tidspunkt,
        )

        false -> UtbetalingskontrollDTO.UtbetalingskontrollMedEndringDTO(
            tidspunkt = this.tidspunkt,
            simulertBeregning = this.toSimulertBeregning(beregninger).toSimulertBeregningDTO(),
        )
    }
}
