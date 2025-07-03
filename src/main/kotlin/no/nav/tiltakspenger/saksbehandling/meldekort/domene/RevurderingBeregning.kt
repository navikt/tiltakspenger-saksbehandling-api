package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList

data class RevurderingBeregning(
    override val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : Utbetalingsberegning,
    List<MeldeperiodeBeregning> by beregninger {

    init {
        require(beregninger.all { it.beregningKilde is MeldeperiodeBeregning.FraBehandling })

        super.init()
    }
}
