package no.nav.tiltakspenger.saksbehandling.meldekort.domene

enum class MeldekortBehandlingStatus {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    AUTOMATISK_BEHANDLET,
    IKKE_RETT_TIL_TILTAKSPENGER,
    AVBRUTT,
}
