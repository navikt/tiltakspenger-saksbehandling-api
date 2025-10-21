package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import java.time.LocalDateTime

/** Abn: kanskje [MeldeperiodeBeregning] kunne holde på iverksatt tidspunkt selv? */
private typealias BeregningMedIverksattTidspunkt = Pair<MeldeperiodeBeregning, LocalDateTime>

/**
 *  Denne skal kun omfatte beregninger som er en del av et vedtak
 * */
data class MeldeperiodeBeregningerVedtatt private constructor(
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

    val gjeldendeBeregningPerKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> by lazy {
        beregningerPerKjede.entries.associate { it.key to it.value.last() }
    }

    val gjeldendeBeregninger: List<MeldeperiodeBeregning> by lazy {
        gjeldendeBeregningPerKjede.values.toList()
    }

    /**
     *  Henter siste beregning før [beregningId] på [kjedeId]
     *
     *  @return [ForrigeBeregningFinnesIkke.IngenTidligereBeregninger] dersom beregningen til [beregningId] er første beregning på kjeden.
     *  [ForrigeBeregningFinnesIkke.BeregningFinnesIkke] dersom beregningen til [beregningId] ikke finnes på kjeden
     * */
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
        return gjeldendeBeregningPerKjede.values.filter { it.periode.overlapperMed(periode) }
    }

    init {
        val duplikater = meldeperiodeBeregninger.nonDistinctBy { it.id }
        require(duplikater.isEmpty()) {
            "Fant duplikate meldeperiodeberegninger: $duplikater"
        }
    }

    companion object {

        fun fraVedtaksliste(vedtaksliste: Vedtaksliste): MeldeperiodeBeregningerVedtatt {
            return MeldeperiodeBeregningerVedtatt(
                vedtaksliste.flatMap { vedtak ->
                    vedtak.beregning?.beregninger?.map { it to vedtak.opprettet } ?: emptyList()
                },
            )
        }
    }

    enum class ForrigeBeregningFinnesIkke {
        IngenBeregningerForKjede,
        IngenTidligereBeregninger,
        BeregningFinnesIkke,
    }
}
