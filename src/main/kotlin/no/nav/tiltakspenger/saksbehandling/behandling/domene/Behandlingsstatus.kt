package no.nav.tiltakspenger.saksbehandling.behandling.domene

/**
 * Kun tenkt brukt for Søknadsbehandling i første omgang.
 * Det kan hende den passer for Revurdering også, men vurderer det når vi kommer dit.
 */
enum class Behandlingsstatus {
    /** Behandlingen er opprettet og blir forsøkt behandlet automatisk */
    UNDER_AUTOMATISK_BEHANDLING,

    /** Det står ikke en saksbehandler på behandlingen */
    KLAR_TIL_BEHANDLING,

    /** En saksbehandler står på behandlingen. Kan også være underkjent. */
    UNDER_BEHANDLING,

    /** Saksbehandler har sendt til beslutning, men ingen beslutter er knyttet til behandlingen enda */
    KLAR_TIL_BESLUTNING,

    /** En beslutter har tatt behandlingen. */
    UNDER_BESLUTNING,

    /** En avsluttet, besluttet behandling. Brukes litt om hverandre med IVERKSATT. */
    VEDTATT,

    /** En saksbehandler har valgt at behandlingen ikke skal behandles videre */
    AVBRUTT,
}
