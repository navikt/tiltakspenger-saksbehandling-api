package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningMedSimulering
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering

/**
 * @param simulering Vi gjør bare en best-effort på å simulere.
 */
data class BehandlingUtbetaling(
    override val beregning: Beregning,
    val navkontor: Navkontor,
    override val simulering: Simulering?,
) : BeregningMedSimulering
