package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær

enum class ReduksjonAvYtelsePåGrunnAvFraværDTO {
    INGEN_REDUKSJON,

    // Kommentar jah: Dersom denne endres fra nåværende prosent på 75% på denne endres fra en enum til et sealed interface også må prosenten legges inn i denne dataklassen.
    DELVIS_REDUKSJON,
    YTELSEN_FALLER_BORT,
}

fun ReduksjonAvYtelsePåGrunnAvFravær.toDTO(): ReduksjonAvYtelsePåGrunnAvFraværDTO =
    when (this) {
        ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon -> ReduksjonAvYtelsePåGrunnAvFraværDTO.INGEN_REDUKSJON
        ReduksjonAvYtelsePåGrunnAvFravær.DelvisReduksjon -> ReduksjonAvYtelsePåGrunnAvFraværDTO.DELVIS_REDUKSJON
        ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort -> ReduksjonAvYtelsePåGrunnAvFraværDTO.YTELSEN_FALLER_BORT
    }
