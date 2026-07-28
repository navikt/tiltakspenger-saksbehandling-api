package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.KanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilKanIkkeIverksetteUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilMeldingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.SimulertBeregningDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.toSimulertBeregningDTO

data class BehandlingUtbetalingDTO(
    val navkontor: String,
    val navkontorNavn: String?,
    val status: UtbetalingsstatusDTO,
    val simulertBeregning: SimulertBeregningDTO,
    val kanIkkeIverksetteUtbetaling: KanIkkeIverksetteUtbetalingDTO?,

    /**
     * Melding fra domenet som kan vises direkte til saksbehandler.
     * Null når grunnen alene er dekkende.
     */
    val kanIkkeIverksetteUtbetalingMelding: String?,
    val tilbakekrevingId: String?,
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
    tilbakekrevingId: TilbakekrevingId?,
): BehandlingUtbetalingDTO {
    val kanIkkeIverksette = this.simulering?.validerKanIverksetteUtbetaling()?.leftOrNull()
    return BehandlingUtbetalingDTO(
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        status = utbetalingsstatus.toUtbetalingsstatusDTO(),
        simulertBeregning = this.toSimulertBeregning(beregninger).toSimulertBeregningDTO(),
        kanIkkeIverksetteUtbetaling = kanIkkeIverksette?.tilKanIkkeIverksetteUtbetalingDTO(),
        kanIkkeIverksetteUtbetalingMelding = kanIkkeIverksette?.tilMeldingDTO(),
        tilbakekrevingId = tilbakekrevingId?.toString(),
    )
}
