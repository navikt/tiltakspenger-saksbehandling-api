package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus

enum class RammebehandlingsstatusDTO {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    VEDTATT,
    AVBRUTT,
}

fun Rammebehandlingsstatus.toBehandlingsstatusDTO(): RammebehandlingsstatusDTO {
    return when (this) {
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> RammebehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> RammebehandlingsstatusDTO.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.UNDER_BEHANDLING -> RammebehandlingsstatusDTO.UNDER_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> RammebehandlingsstatusDTO.UNDER_BESLUTNING
        Rammebehandlingsstatus.VEDTATT -> RammebehandlingsstatusDTO.VEDTATT
        Rammebehandlingsstatus.AVBRUTT -> RammebehandlingsstatusDTO.AVBRUTT
    }
}
