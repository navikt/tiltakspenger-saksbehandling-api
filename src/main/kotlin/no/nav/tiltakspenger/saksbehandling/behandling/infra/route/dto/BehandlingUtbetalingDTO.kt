package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
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

fun BehandlingUtbetaling.tilDTO(
    utbetalingsstatus: Utbetalingsstatus?,
    tidligereUtbetalinger: Utbetalinger,
): BehandlingUtbetalingDTO {
    return BehandlingUtbetalingDTO(
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        status = utbetalingsstatus.toUtbetalingsstatusDTO(),
        simulertBeregning = this.toSimulertBeregning(tidligereUtbetalinger).toSimulertBeregningDTO(),
    )
}
