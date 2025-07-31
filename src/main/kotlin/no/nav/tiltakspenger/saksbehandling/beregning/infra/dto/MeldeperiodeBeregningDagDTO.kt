package no.nav.tiltakspenger.saksbehandling.beregning.infra.dto

import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortDagStatusDTO
import java.time.LocalDate

data class MeldeperiodeBeregningDagDTO(
    val dato: LocalDate,
    val status: MeldekortDagStatusDTO,
    val reduksjonAvYtelsePåGrunnAvFravær: ReduksjonAvYtelsePåGrunnAvFraværDTO?,
    val beregningsdag: BeregningsdagDTO?,
)

fun MeldeperiodeBeregning.tilMeldeperiodeBeregningDagerDTO(): List<MeldeperiodeBeregningDagDTO> = this.dager.map {
    MeldeperiodeBeregningDagDTO(
        dato = it.dato,
        status = it.tilMeldekortDagStatusDTO(),
        reduksjonAvYtelsePåGrunnAvFravær = it.reduksjon.toReduksjonAvYtelsePåGrunnAvFraværDTO(),
        beregningsdag = it.beregningsdag?.toBeregningsdagDTO(),
    )
}
