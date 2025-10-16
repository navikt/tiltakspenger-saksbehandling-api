package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import java.time.LocalDateTime

/** Abn: kanskje [MeldeperiodeBeregning] burde holde på iverksatt tidspunkt selv */
private typealias BeregningMedIverksattTidspunkt = Pair<MeldeperiodeBeregning, LocalDateTime>

/**
 *  Denne skal kun omfatte beregninger som er en del av en vedtatt utbetaling.
 * */
data class MeldeperiodeBeregninger private constructor(
    private val meldeperiodeBeregningerMedTidspunkt: List<BeregningMedIverksattTidspunkt>,
) {

    private val meldeperiodeBeregninger: List<MeldeperiodeBeregning> by lazy {
        meldeperiodeBeregningerMedTidspunkt.sortedBy { it.second }.map { it.first }
    }

    val beregningerPerKjede: Map<MeldeperiodeKjedeId, NonEmptyList<MeldeperiodeBeregning>> by lazy {
        meldeperiodeBeregninger
            .groupBy { it.kjedeId }
            .mapValues { it.value.toNonEmptyListOrThrow() }
    }

    val sisteBeregningPerKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> by lazy {
        beregningerPerKjede.entries.associate { it.key to it.value.last() }
    }

    val gjeldendeBeregninger: List<MeldeperiodeBeregning> by lazy {
        sisteBeregningPerKjede.values.toList()
    }

    fun hentForrigeBeregning(
        beregningId: BeregningId,
        kjedeId: MeldeperiodeKjedeId,
    ): Either<ForrigeBeregningFinnesIkke, MeldeperiodeBeregning> {
        val beregningerForKjede =
            beregningerPerKjede[kjedeId] ?: return ForrigeBeregningFinnesIkke.IngenBeregningerForKjede.left()

        // Finnes ingen forrige beregning hvis dette er den første på kjeden
        if (beregningerForKjede.first().id == beregningId) {
            return ForrigeBeregningFinnesIkke.IngenTidligereBeregninger.left()
        }

        return beregningerForKjede.takeWhile { it.id != beregningId }.let {
            if (it.isEmpty()) {
                ForrigeBeregningFinnesIkke.BeregningFinnesIkke.left()
            } else {
                it.last().right()
            }
        }
    }

    fun sisteBeregningerForPeriode(periode: Periode): List<MeldeperiodeBeregning> {
        return sisteBeregningPerKjede.values.filter { it.periode.overlapperMed(periode) }
    }

    init {
        val duplikater = meldeperiodeBeregninger.nonDistinctBy { it.id }
        require(duplikater.isEmpty()) {
            "Fant duplikate meldeperiodeberegninger: $duplikater"
        }
    }

    // Tanken var å bruke disse til tester for å skille mellom ulike null-resultater.
    // TODO etter omskrivning av MeldeperiodeBeregninger :D
    enum class ForrigeBeregningFinnesIkke {
        IngenBeregningerForKjede,
        IngenTidligereBeregninger,
        BeregningFinnesIkke,
    }

    companion object {

        fun fraUtbetalinger(utbetalinger: Utbetalinger): MeldeperiodeBeregninger {
            return MeldeperiodeBeregninger(
                utbetalinger.verdi.flatMap { utbetaling ->
                    utbetaling.beregning.map { it to utbetaling.opprettet }
                },
            )
        }
    }
}
