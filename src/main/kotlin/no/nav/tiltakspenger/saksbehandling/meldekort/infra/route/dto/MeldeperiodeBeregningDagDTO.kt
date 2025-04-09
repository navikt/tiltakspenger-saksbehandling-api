package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import java.time.LocalDate

data class MeldeperiodeBeregningDagDTO(
    val dato: LocalDate,
    val status: MeldekortDagStatusDTO,
    val reduksjonAvYtelsePåGrunnAvFravær: ReduksjonAvYtelsePåGrunnAvFraværDTO?,
    val beregningsdag: BeregningsdagDTO?,
)

fun MeldeperiodeBeregning.tilMeldeperiodeBeregningDagerDTO() = this.map {
    MeldeperiodeBeregningDagDTO(
        dato = it.dato,
        status = it.tilMeldekortDagStatusDTO(),
        reduksjonAvYtelsePåGrunnAvFravær = it.reduksjon.toReduksjonAvYtelsePåGrunnAvFraværDTO(),
        beregningsdag = it.beregningsdag?.toBeregningsdagDTO(),
    )
}
