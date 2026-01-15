package no.nav.tiltakspenger.saksbehandling.klage.domene

enum class Klagebehandlingsstatus {
    /** Det står ikke en saksbehandler på behandlingen */
    KLAR_TIL_BEHANDLING,

    /** En saksbehandler står på behandlingen. */
    UNDER_BEHANDLING,

    AVBRUTT,
    IVERKSATT,
}
