package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import java.time.LocalDate

data class BehandlingBeregning(
    override val beregninger: NonEmptyList<MeldeperiodeBeregning>,
) : UtbetalingBeregning,
    List<MeldeperiodeBeregning> by beregninger {

    init {
        require(beregninger.all { it.beregningKilde is BeregningKilde.BeregningKildeBehandling })

        super.init()
    }

    fun finnBeløpDiff(meldeperiodeBeregninger: MeldeperiodeBeregninger): Int {
        val forrigeBeregninger: List<MeldeperiodeBeregning> =
            beregninger.mapNotNull { meldeperiodeBeregninger.sisteBeregningFør(it.id, it.kjedeId) }

        return beregninger.beregnTotalBeløp() - forrigeBeregninger.beregnTotalBeløp()
    }

    override fun hentDag(dato: LocalDate): MeldeperiodeBeregningDag? {
        return beregninger.mapNotNull { it.hentDag(dato) }.singleOrNullOrThrow()
    }
}
