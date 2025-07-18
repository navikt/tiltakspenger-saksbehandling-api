package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger

data class MeldeperiodeBeregninger(
    val meldekortBehandlinger: MeldekortBehandlinger,
) {
    private val godkjenteMeldekort = meldekortBehandlinger.godkjenteMeldekort
        .sortedBy { it.iverksattTidspunkt }

    private val meldeperiodeBeregninger: List<MeldeperiodeBeregning> by lazy {
        godkjenteMeldekort.flatMap { it.beregning.beregninger }
    }

    val beregningerPerKjede: Map<MeldeperiodeKjedeId, List<MeldeperiodeBeregning>> by lazy {
        meldeperiodeBeregninger.groupBy { it.kjedeId }
    }

    val sisteBeregningPerKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> by lazy {
        beregningerPerKjede.entries.associate { it.key to it.value.last() }
    }

    fun sisteBeregningFør(beregningId: BeregningId, kjedeId: MeldeperiodeKjedeId): MeldeperiodeBeregning? {
        return beregningerPerKjede[kjedeId]?.takeWhile { it.id != beregningId }?.lastOrNull()
    }

    fun sisteBeregningerForPeriode(periode: Periode): List<MeldeperiodeBeregning> {
        return sisteBeregningPerKjede.values.filter { it.periode.overlapperMed(periode) }
    }

    init {
        godkjenteMeldekort.zipWithNext { a, b ->
            require(a.iverksattTidspunkt!! < b.iverksattTidspunkt!!) {
                "Meldekortene må ha unike iverksatt tidspunkt og være sorterte - Fant ${a.id} ${a.iverksattTidspunkt} / ${b.id} ${b.iverksattTidspunkt}"
            }
        }
    }
}
