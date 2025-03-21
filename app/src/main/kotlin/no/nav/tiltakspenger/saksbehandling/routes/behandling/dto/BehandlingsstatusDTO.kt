package no.nav.tiltakspenger.saksbehandling.routes.behandling.dto

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus

enum class BehandlingsstatusDTO {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    VEDTATT,
    AVBRUTT,
}

fun Behandlingsstatus.toDTO(): BehandlingsstatusDTO {
    return when (this) {
        Behandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingsstatusDTO.KLAR_TIL_BEHANDLING
        Behandlingsstatus.UNDER_BEHANDLING -> BehandlingsstatusDTO.UNDER_BEHANDLING
        Behandlingsstatus.KLAR_TIL_BESLUTNING -> BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
        Behandlingsstatus.UNDER_BESLUTNING -> BehandlingsstatusDTO.UNDER_BESLUTNING
        Behandlingsstatus.VEDTATT -> BehandlingsstatusDTO.VEDTATT
        Behandlingsstatus.AVBRUTT -> BehandlingsstatusDTO.AVBRUTT
    }
}
