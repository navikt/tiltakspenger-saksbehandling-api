package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering

/**
 * @param simulering Vi gjør bare en best-effort på å simulere.
 */
data class BehandlingUtbetaling(
    val beregning: BehandlingBeregning,
    val navkontor: Navkontor,
    val simulering: Simulering?,
) {
    fun oppdaterSimulering(nySimulering: Simulering?) = copy(simulering = nySimulering)
}
