package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus

enum class BehandlingsstatusDTO {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    VEDTATT,
    AVBRUTT,
}

fun Behandlingsstatus.toBehandlingsstatusDTO(): BehandlingsstatusDTO {
    return when (this) {
        Behandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingsstatusDTO.KLAR_TIL_BEHANDLING
        Behandlingsstatus.UNDER_BEHANDLING -> BehandlingsstatusDTO.UNDER_BEHANDLING
        Behandlingsstatus.KLAR_TIL_BESLUTNING -> BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
        Behandlingsstatus.UNDER_BESLUTNING -> BehandlingsstatusDTO.UNDER_BESLUTNING
        Behandlingsstatus.VEDTATT -> BehandlingsstatusDTO.VEDTATT
        Behandlingsstatus.AVBRUTT -> BehandlingsstatusDTO.AVBRUTT
    }
}
