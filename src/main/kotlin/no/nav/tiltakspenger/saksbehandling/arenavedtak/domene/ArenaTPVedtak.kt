package no.nav.tiltakspenger.saksbehandling.arenavedtak.domene

import java.time.LocalDate

data class ArenaTPVedtak(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val rettighet: Rettighet,
    val vedtakId: Long,
) {
    enum class Rettighet {
        TILTAKSPENGER,
        BARNETILLEGG,
        TILTAKSPENGER_OG_BARNETILLEGG,
        INGENTING,
    }
}
