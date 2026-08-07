package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

/**
 * Delt status for behandlingstypene som går gjennom den vanlige saksbehandlingsflyten.
 * Gjelder søknader, revurderinger, meldekort og klage.
 * Tilbakekreving har sin egen flyt, og dermed sin egen status i [BenkTilbakekrevingStatus].
 */
enum class BenkV2Behandlingsstatus {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    KLAR_TIL_FERDIGSTILLING,
}
