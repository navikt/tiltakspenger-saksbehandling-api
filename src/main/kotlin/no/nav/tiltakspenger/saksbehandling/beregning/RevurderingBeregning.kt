package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList

data class RevurderingBeregning(
    override val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : Utbetalingsberegning,
    List<MeldeperiodeBeregning> by beregninger {

    init {
        require(beregninger.all { it.beregningKilde is BeregningKilde.Behandling })

        super.init()
    }
}
