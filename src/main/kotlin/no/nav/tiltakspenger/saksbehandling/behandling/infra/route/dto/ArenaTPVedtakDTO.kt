package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import java.time.LocalDate

data class ArenaTPVedtakDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val rettighet: String,
)

fun ArenaTPVedtak.toDto(): ArenaTPVedtakDTO = ArenaTPVedtakDTO(
    fraOgMed = this.fraOgMed,
    tilOgMed = this.tilOgMed,
    rettighet = when (this.rettighet) {
        ArenaTPVedtak.Rettighet.TILTAKSPENGER -> "Tiltakspenger"
        ArenaTPVedtak.Rettighet.BARNETILLEGG -> "Barnetillegg"
        ArenaTPVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG -> "Tiltakspenger og barnetillegg"
        ArenaTPVedtak.Rettighet.INGENTING -> "Stanset vedtak"
    },
)
