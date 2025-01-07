package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregning
import java.time.LocalDate

data class MeldekortDagDTO(
    val dato: LocalDate,
    val status: String,
    val reduksjonAvYtelsePåGrunnAvFravær: ReduksjonAvYtelsePåGrunnAvFraværDTO?,
    val beregningsdag: BeregningsdagDTO?,
)

fun MeldeperiodeBeregning.toDTO(): List<MeldekortDagDTO> =
    this.map {
        MeldekortDagDTO(
            dato = it.dato,
            status = it.toStatusDTO().toString(),
            reduksjonAvYtelsePåGrunnAvFravær = it.reduksjon?.toDTO(),
            beregningsdag = it.beregningsdag?.toDTO(),
        )
    }
