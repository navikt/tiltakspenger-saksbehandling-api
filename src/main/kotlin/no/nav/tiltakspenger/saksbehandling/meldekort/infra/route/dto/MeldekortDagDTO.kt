package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning
import java.time.LocalDate

data class MeldekortDagDTO(
    val dato: LocalDate,
    val status: String,
    val reduksjonAvYtelsePåGrunnAvFravær: ReduksjonAvYtelsePåGrunnAvFraværDTO?,
    val beregningsdag: BeregningsdagDTO?,
)

fun MeldekortBeregning.toMeldekortDagDTO(): List<MeldekortDagDTO> =
    this.map {
        MeldekortDagDTO(
            dato = it.dato,
            status = it.toStatusDTO().toString(),
            reduksjonAvYtelsePåGrunnAvFravær = it.reduksjon?.toReduksjonAvYtelsePåGrunnAvFraværDTO(),
            beregningsdag = it.beregningsdag?.toBeregningsdagDTO(),
        )
    }
