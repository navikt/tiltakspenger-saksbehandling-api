package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak.Rettighet
import java.time.LocalDate

data class ArenaTPVedtakDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val rettighet: Rettighet,
)
