package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregningOld

data class MeldeperiodeBeregnetDTO(
    val kjedeId: String,
    val meldekortId: String,
    val dager: List<MeldekortDagDTO>,
)

fun MeldekortBeregningOld.toMeldekortBeregningDTO(): List<MeldeperiodeBeregnetDTO>? {
    if (this !is MeldekortBeregningOld.UtfyltMeldeperiode) {
        return null
    }

    return this.beregninger.toList().map {
        MeldeperiodeBeregnetDTO(
            kjedeId = it.kjedeId.toString(),
            meldekortId = it.meldekortId.toString(),
            dager = it.dager.toMeldekortDagerDTO(),
        )
    }
}
