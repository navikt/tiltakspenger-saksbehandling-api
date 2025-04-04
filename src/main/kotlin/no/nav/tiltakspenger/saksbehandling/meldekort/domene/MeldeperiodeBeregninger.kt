package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId

data class MeldeperiodeBeregninger(
    val meldekortBehandlinger: MeldekortBehandlinger,
) {
    private val godkjenteMeldekort = meldekortBehandlinger.godkjenteMeldekort
        .sortedBy { it.iverksattTidspunkt }

    private val meldeperiodeBeregninger = godkjenteMeldekort
        .flatMap { it.beregning.beregninger }

    val beregningerForKjede: Map<MeldeperiodeKjedeId, List<MeldeperiodeBeregning>> =
        meldeperiodeBeregninger.groupBy { it.kjedeId }

    val sisteBeregningForKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> =
        beregningerForKjede.entries.associate { it.key to it.value.last() }

    val beregningerForMeldekort: Map<MeldekortId, List<MeldeperiodeBeregning>> =
        meldeperiodeBeregninger.groupBy { it.meldekortId }

    init {
        godkjenteMeldekort.zipWithNext { a, b ->
            require(a.iverksattTidspunkt!! < b.iverksattTidspunkt!!) {
                "Meldekortene må ha unike iverksatt tidspunkt og være sorterte - Fant ${a.id} ${a.iverksattTidspunkt} / ${b.id} ${b.iverksattTidspunkt}"
            }
        }
    }
}
