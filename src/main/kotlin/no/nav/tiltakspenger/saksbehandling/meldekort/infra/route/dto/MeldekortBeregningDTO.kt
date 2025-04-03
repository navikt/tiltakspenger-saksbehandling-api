package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning

data class MeldeperiodeBeregnetDTO(
    val kjedeId: String,
    val meldekortId: String,
    val dager: List<MeldeperiodeBeregningDagDTO>,
)

fun MeldekortBeregning.toMeldekortBeregningDTO(): List<MeldeperiodeBeregnetDTO> {
    return this.toList().map {
        MeldeperiodeBeregnetDTO(
            kjedeId = it.kjedeId.toString(),
            meldekortId = it.meldekortId.toString(),
            dager = it.dager.toMeldeperiodeBeregningDagerDTO(),
        )
    }
}
