package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import java.time.LocalDate

data class ArenaTPVedtakDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val rettighet: String,
)
