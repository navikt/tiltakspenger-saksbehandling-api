package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering

/**
 *  En ny beregning og simulering som utføres når behandlingen sendes til beslutning eller iverksettes.
 *
 *  TODO abn: denne burde også benyttes for meldekortbehandlinger
 * */
data class Utbetalingskontroll(
    val beregning: Beregning,
    val simulering: Simulering,
)
