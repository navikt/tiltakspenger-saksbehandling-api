package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import kotlin.collections.first

data class MeldekortBeregning(
    override val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : UtbetalingBeregning,
    List<MeldeperiodeBeregning> by beregninger {

    val beregningForMeldekortetsPeriode by lazy { beregninger.first() }
    val beregningerForPåfølgendePerioder by lazy { beregninger.drop(1) }

    val dagerFraMeldekortet by lazy { beregningForMeldekortetsPeriode.dager }

    init {
        require(beregninger.all { it.beregningKilde is BeregningKilde.Meldekort })

        super.init()
    }
}
