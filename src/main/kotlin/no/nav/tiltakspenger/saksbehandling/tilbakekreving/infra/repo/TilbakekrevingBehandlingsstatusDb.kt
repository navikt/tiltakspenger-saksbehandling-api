package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus

enum class TilbakekrevingBehandlingsstatusDb {
    OPPRETTET,
    TIL_FORHÅNDSVARSEL,
    TIL_BEHANDLING,
    TIL_GODKJENNING,
    AVSLUTTET,
    ;

    fun tilDomene(): TilbakekrevingBehandlingsstatus = when (this) {
        OPPRETTET -> TilbakekrevingBehandlingsstatus.OPPRETTET
        TIL_FORHÅNDSVARSEL -> TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL
        TIL_BEHANDLING -> TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
        TIL_GODKJENNING -> TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
        AVSLUTTET -> TilbakekrevingBehandlingsstatus.AVSLUTTET
    }
}

fun TilbakekrevingBehandlingsstatus.tilDb(): TilbakekrevingBehandlingsstatusDb = when (this) {
    TilbakekrevingBehandlingsstatus.OPPRETTET -> TilbakekrevingBehandlingsstatusDb.OPPRETTET
    TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL -> TilbakekrevingBehandlingsstatusDb.TIL_FORHÅNDSVARSEL
    TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> TilbakekrevingBehandlingsstatusDb.TIL_BEHANDLING
    TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> TilbakekrevingBehandlingsstatusDb.TIL_GODKJENNING
    TilbakekrevingBehandlingsstatus.AVSLUTTET -> TilbakekrevingBehandlingsstatusDb.AVSLUTTET
}

fun TilbakekrevingBehandlingsstatus.tilDbString(): String = tilDb().name
