package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

enum class TilbakekrevingBehandlingsstatus {
    OPPRETTET,
    TIL_BEHANDLING,
    TIL_GODKJENNING,
    AVSLUTTET,
}

enum class TilbakekrevingBehandlingsstatusIntern {
    OPPRETTET,
    TIL_BEHANDLING,
    UNDER_BEHANDLING,
    TIL_GODKJENNING,
    UNDER_GODKJENNING,
    AVSLUTTET,
}
