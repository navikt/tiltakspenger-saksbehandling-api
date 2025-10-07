package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.first

data class MeldekortBeregning(
    override val beregninger: NonEmptyList<MeldeperiodeBeregning>,
    override val beregningstidspunkt: LocalDateTime?,
) : UtbetalingBeregning,
    List<MeldeperiodeBeregning> by beregninger {

    val beregningForMeldekortetsPeriode by lazy { beregninger.first() }
    val beregningerForPåfølgendePerioder by lazy { beregninger.drop(1) }

    val dagerFraMeldekortet by lazy { beregningForMeldekortetsPeriode.dager }

    override fun hentDag(dato: LocalDate): MeldeperiodeBeregningDag? {
        return beregninger.mapNotNull { it.hentDag(dato) }.singleOrNullOrThrow()
    }
    init {
        require(beregninger.all { it.beregningKilde is BeregningKilde.BeregningKildeMeldekort })

        super.init()
    }
}
