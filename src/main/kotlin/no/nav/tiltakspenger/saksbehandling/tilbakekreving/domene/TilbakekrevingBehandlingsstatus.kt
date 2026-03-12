package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingBehandlingEndretDTO.TilbakekrevingBehandlingsstatusDTO

enum class TilbakekrevingBehandlingsstatus {
    OPPRETTET,
    TIL_BEHANDLING,
    TIL_GODKJENNING,
    AVSLUTTET,
}

fun TilbakekrevingBehandlingsstatusDTO.tilTilbakekrevingBehandlingsstatus(): TilbakekrevingBehandlingsstatus {
    return when (this) {
        TilbakekrevingBehandlingsstatusDTO.OPPRETTET -> TilbakekrevingBehandlingsstatus.OPPRETTET
        TilbakekrevingBehandlingsstatusDTO.TIL_BEHANDLING -> TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TilbakekrevingBehandlingsstatusDTO.TIL_GODKJENNING -> TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
        TilbakekrevingBehandlingsstatusDTO.AVSLUTTET -> TilbakekrevingBehandlingsstatus.AVSLUTTET
    }
}
