package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor

data class BehandlingUtbetaling(
    val beregning: BehandlingBeregning,
    val navkontor: Navkontor,
)
