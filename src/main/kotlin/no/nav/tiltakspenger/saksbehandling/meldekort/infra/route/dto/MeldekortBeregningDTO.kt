package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning

data class MeldeperiodeBeregningDTO(
    val kjedeId: String,
    val meldekortId: String,
    val dager: List<MeldeperiodeBeregningDagDTO>,
)

fun MeldekortBeregning.toMeldekortBeregningDTO(): List<MeldeperiodeBeregningDTO> {
    return this.toList().map {
        MeldeperiodeBeregningDTO(
            kjedeId = it.kjedeId.toString(),
            meldekortId = it.meldekortId.toString(),
            dager = it.dager.toMeldeperiodeBeregningDagerDTO(),
        )
    }
}
