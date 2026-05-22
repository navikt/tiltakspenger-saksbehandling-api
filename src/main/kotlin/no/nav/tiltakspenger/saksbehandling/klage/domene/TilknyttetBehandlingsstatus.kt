package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

enum class TilknyttetBehandlingsstatus {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    VEDTATT,
    GODKJENT,
    AUTOMATISK_BEHANDLET,
    AVBRUTT,
}

fun Rammebehandlingsstatus.tilTilknyttetBehandlingsstatus(): TilknyttetBehandlingsstatus {
    return when (this) {
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> TilknyttetBehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> TilknyttetBehandlingsstatus.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.UNDER_BEHANDLING -> TilknyttetBehandlingsstatus.UNDER_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> TilknyttetBehandlingsstatus.KLAR_TIL_BESLUTNING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> TilknyttetBehandlingsstatus.UNDER_BESLUTNING
        Rammebehandlingsstatus.VEDTATT -> TilknyttetBehandlingsstatus.VEDTATT
        Rammebehandlingsstatus.AVBRUTT -> TilknyttetBehandlingsstatus.AVBRUTT
    }
}

fun MeldekortbehandlingStatus.tilTilknyttetBehandlingsstatus(): TilknyttetBehandlingsstatus {
    return when (this) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> TilknyttetBehandlingsstatus.KLAR_TIL_BEHANDLING
        MeldekortbehandlingStatus.UNDER_BEHANDLING -> TilknyttetBehandlingsstatus.UNDER_BEHANDLING
        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> TilknyttetBehandlingsstatus.KLAR_TIL_BESLUTNING
        MeldekortbehandlingStatus.UNDER_BESLUTNING -> TilknyttetBehandlingsstatus.UNDER_BESLUTNING
        MeldekortbehandlingStatus.GODKJENT -> TilknyttetBehandlingsstatus.GODKJENT
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> TilknyttetBehandlingsstatus.AUTOMATISK_BEHANDLET
        MeldekortbehandlingStatus.AVBRUTT -> TilknyttetBehandlingsstatus.AVBRUTT
    }
}
