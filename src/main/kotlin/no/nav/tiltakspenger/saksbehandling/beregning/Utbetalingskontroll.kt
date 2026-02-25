package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.LocalDateTime

/**
 *  En ny beregning og simulering som utføres når behandlingen sendes til beslutning eller iverksettes.
 *
 *  TODO abn: denne burde også benyttes for meldekortbehandlinger
 * */
data class Utbetalingskontroll(
    override val beregning: Beregning,
    override val simulering: Simulering,
) : BeregningMedSimulering {
    val tidspunkt: LocalDateTime = simulering.simuleringstidspunkt
}
