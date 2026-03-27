package no.nav.tiltakspenger.saksbehandling.beregning.infra.dto

import no.nav.tiltakspenger.saksbehandling.beregning.ReduksjonAvYtelsePåGrunnAvFravær

enum class ReduksjonAvYtelsePåGrunnAvFraværDTO {
    INGEN_REDUKSJON,

    // Kommentar jah: Dersom denne endres fra nåværende prosent på 75% må denne endres fra en enum til et sealed interface også må prosenten legges inn i denne dataklassen.
    DELVIS_REDUKSJON,
    YTELSEN_FALLER_BORT,
}

fun ReduksjonAvYtelsePåGrunnAvFravær.toReduksjonAvYtelsePåGrunnAvFraværDTO(): ReduksjonAvYtelsePåGrunnAvFraværDTO =
    when (this) {
        ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon -> ReduksjonAvYtelsePåGrunnAvFraværDTO.INGEN_REDUKSJON
        ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon -> ReduksjonAvYtelsePåGrunnAvFraværDTO.DELVIS_REDUKSJON
        ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort -> ReduksjonAvYtelsePåGrunnAvFraværDTO.YTELSEN_FALLER_BORT
    }
