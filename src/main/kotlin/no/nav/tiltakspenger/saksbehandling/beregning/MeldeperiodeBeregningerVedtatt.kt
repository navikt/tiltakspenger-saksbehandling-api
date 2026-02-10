package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste

/**
 *  Denne skal kun omfatte beregninger som er en del av et vedtak
 * */
data class MeldeperiodeBeregningerVedtatt private constructor(
    private val meldeperiodeBeregninger: List<MeldeperiodeBeregning>,
) {

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
     *  @return
     *  [ForrigeBeregningFinnesIkke.IngenBeregningerForKjede] dersom kjeden til [kjedeId] ikke har noen beregninger.
     *  [ForrigeBeregningFinnesIkke.IngenTidligereBeregninger] dersom beregningen til [beregningId] er første beregning på kjeden.
     *  [ForrigeBeregningFinnesIkke.BeregningFinnesIkke] dersom beregningen til [beregningId] ikke finnes på kjeden.
     * */
    fun hentForrigeBeregning(
        beregningId: BeregningId,
        kjedeId: MeldeperiodeKjedeId,
    ): Either<ForrigeBeregningFinnesIkke, MeldeperiodeBeregning> {
        val beregningerForKjede =
            beregningerPerKjede[kjedeId] ?: return ForrigeBeregningFinnesIkke.IngenBeregningerForKjede.left()

        val beregningIndex = beregningerForKjede.indexOfFirst { it.id == beregningId }

        if (beregningIndex == -1) {
            return ForrigeBeregningFinnesIkke.BeregningFinnesIkke.left()
        }

        if (beregningIndex == 0) {
            return ForrigeBeregningFinnesIkke.IngenTidligereBeregninger.left()
        }

        return beregningerForKjede[beregningIndex - 1].right()
    }

    /**
     * Henter forrige beregning på [kjedeId] før [beregningId], eller siste gjeldende beregning på kjeden dersom
     * beregningen med [beregningId] ikke finnes.
     */
    fun hentForrigeBeregningEllerSiste(
        beregningId: BeregningId,
        kjedeId: MeldeperiodeKjedeId,
    ): MeldeperiodeBeregning? = hentForrigeBeregning(beregningId, kjedeId).getOrElse {
        when (it) {
            ForrigeBeregningFinnesIkke.IngenBeregningerForKjede,
            ForrigeBeregningFinnesIkke.IngenTidligereBeregninger,
            -> null

            ForrigeBeregningFinnesIkke.BeregningFinnesIkke -> gjeldendeBeregningPerKjede[kjedeId]
        }
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
                (vedtaksliste.rammevedtaksliste + vedtaksliste.meldekortvedtaksliste)
                    .sortedBy { it.opprettet }
                    .flatMap { it.beregning?.beregninger ?: emptyList() },
            )
        }
    }

    enum class ForrigeBeregningFinnesIkke {
        IngenBeregningerForKjede,
        IngenTidligereBeregninger,
        BeregningFinnesIkke,
    }
}
