package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.erLik
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO

data class BehandlingUtbetalingDTO(
    val navkontor: String,
    val navkontorNavn: String?,
    val status: UtbetalingsstatusDTO,
    val simulertBeregning: SimulertBeregningDTO,
)

data class BeregningerSummertDTO(
    val totalt: BeløpFørOgNåDTO,
    val ordinært: BeløpFørOgNåDTO,
    val barnetillegg: BeløpFørOgNåDTO,
)

data class BeløpFørOgNåDTO(
    val før: Int?,
    val nå: Int,
)

data class UtbetalingskontrollDTO(
    val simulertBeregning: SimulertBeregningDTO,
    val harEndringer: Boolean,
)

fun BehandlingUtbetaling.tilDTO(
    utbetalingsstatus: Utbetalingsstatus?,
    beregninger: MeldeperiodeBeregningerVedtatt,
): BehandlingUtbetalingDTO {
    return BehandlingUtbetalingDTO(
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        status = utbetalingsstatus.toUtbetalingsstatusDTO(),
        simulertBeregning = this.toSimulertBeregning(beregninger).toSimulertBeregningDTO(),
    )
}

fun Utbetalingskontroll.tilDTO(
    behandlingUtbetaling: BehandlingUtbetaling?,
    beregninger: MeldeperiodeBeregningerVedtatt,
): UtbetalingskontrollDTO {
    return UtbetalingskontrollDTO(
        simulertBeregning = this.toSimulertBeregning(beregninger).toSimulertBeregningDTO(),
        harEndringer = this.simulering.erLik(behandlingUtbetaling?.simulering),
    )
}
