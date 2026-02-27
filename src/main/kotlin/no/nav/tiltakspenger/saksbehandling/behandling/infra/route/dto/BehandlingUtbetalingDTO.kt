package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.KanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilKanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO

data class BehandlingUtbetalingDTO(
    val navkontor: String,
    val navkontorNavn: String?,
    val status: UtbetalingsstatusDTO,
    val simulertBeregning: SimulertBeregningDTO,
    val kanIkkeIverksetteUtbetaling: KanIkkeIverksetteUtbetalingDTO?,
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
    beregninger: MeldeperiodeBeregningerVedtatt,
    valideringsresultat: KanIkkeIverksetteUtbetaling?,
): BehandlingUtbetalingDTO {
    return BehandlingUtbetalingDTO(
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        status = utbetalingsstatus.toUtbetalingsstatusDTO(),
        simulertBeregning = this.toSimulertBeregning(beregninger).toSimulertBeregningDTO(),
        kanIkkeIverksetteUtbetaling = valideringsresultat?.tilKanIkkeIverksetteUtbetalingDTO(),
    )
}
