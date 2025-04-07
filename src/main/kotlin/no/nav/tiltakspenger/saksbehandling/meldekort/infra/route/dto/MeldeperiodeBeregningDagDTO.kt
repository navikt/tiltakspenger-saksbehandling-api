package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import java.time.LocalDate

data class MeldeperiodeBeregningDagDTO(
    val dato: LocalDate,
    val status: MeldekortDagStatusMotFrontendDTO,
    val reduksjonAvYtelsePåGrunnAvFravær: ReduksjonAvYtelsePåGrunnAvFraværDTO?,
    val beregningsdag: BeregningsdagDTO?,
)

fun List<MeldeperiodeBeregningDag>.toMeldeperiodeBeregningDagerDTO() = this.map {
    MeldeperiodeBeregningDagDTO(
        dato = it.dato,
        status = it.tilMeldekortDagStatusDTO(),
        reduksjonAvYtelsePåGrunnAvFravær = it.reduksjon?.toReduksjonAvYtelsePåGrunnAvFraværDTO(),
        beregningsdag = it.beregningsdag?.toBeregningsdagDTO(),
    )
}
