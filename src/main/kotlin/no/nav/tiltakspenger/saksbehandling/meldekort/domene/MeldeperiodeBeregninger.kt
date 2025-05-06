package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId

data class MeldeperiodeBeregninger(
    val meldekortBehandlinger: MeldekortBehandlinger,
) {
    private val godkjenteMeldekort = meldekortBehandlinger.godkjenteMeldekort
        .sortedBy { it.iverksattTidspunkt }

    private val meldeperiodeBeregninger: List<MeldeperiodeBeregning> by lazy {
        godkjenteMeldekort.flatMap { it.beregning.beregninger }
    }

    val beregningerForKjede: Map<MeldeperiodeKjedeId, List<MeldeperiodeBeregning>> by lazy {
        meldeperiodeBeregninger.groupBy { it.kjedeId }
    }

    val sisteBeregningForKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> by lazy {
        beregningerForKjede.entries.associate { it.key to it.value.last() }
    }

    fun sisteBeregningFør(meldekortId: MeldekortId, kjedeId: MeldeperiodeKjedeId): MeldeperiodeBeregning? {
        return beregningerForKjede[kjedeId]?.takeWhile { it.beregningMeldekortId != meldekortId }?.lastOrNull()
    }

    init {
        godkjenteMeldekort.zipWithNext { a, b ->
            require(a.iverksattTidspunkt!! < b.iverksattTidspunkt!!) {
                "Meldekortene må ha unike iverksatt tidspunkt og være sorterte - Fant ${a.id} ${a.iverksattTidspunkt} / ${b.id} ${b.iverksattTidspunkt}"
            }
        }
    }
}
