package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId

data class MeldeperiodeBeregninger(
    val meldekortBehandlinger: MeldekortBehandlinger,
) {
    private val beregningerSortert =
        meldekortBehandlinger.godkjenteMeldekort.flatMap { it.beregning.beregninger }.sortedBy { it.beregnet }

    val beregningerForKjede: Map<MeldeperiodeKjedeId, List<MeldeperiodeBeregning>> =
        beregningerSortert.groupBy { it.kjedeId }

    val sisteBeregningForKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> =
        beregningerForKjede.entries.associate { it.key to it.value.last() }

    val beregningerForMeldekort: Map<MeldekortId, List<MeldeperiodeBeregning>> =
        beregningerSortert.groupBy { it.meldekortId }
}
