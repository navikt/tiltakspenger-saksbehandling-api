package no.nav.tiltakspenger.saksbehandling.benk.domene

/**
 * Delt status for behandlingstypene som går gjennom den vanlige saksbehandlingsflyten.
 * Gjelder søknader, revurderinger og meldekort.
 * Klage og tilbakekreving har egne statuser i [BenkKlagebehandlingStatus] og [BenkTilbakekrevingStatus].
 */
enum class BenkBehandlingsstatus {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
}
