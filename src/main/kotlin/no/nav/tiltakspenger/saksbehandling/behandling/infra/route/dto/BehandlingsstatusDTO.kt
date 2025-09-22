package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus

enum class BehandlingsstatusDTO {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    VEDTATT,
    AVBRUTT,
}

fun Rammebehandlingsstatus.toBehandlingsstatusDTO(): BehandlingsstatusDTO {
    return when (this) {
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> BehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingsstatusDTO.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.UNDER_BEHANDLING -> BehandlingsstatusDTO.UNDER_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> BehandlingsstatusDTO.UNDER_BESLUTNING
        Rammebehandlingsstatus.VEDTATT -> BehandlingsstatusDTO.VEDTATT
        Rammebehandlingsstatus.AVBRUTT -> BehandlingsstatusDTO.AVBRUTT
    }
}
