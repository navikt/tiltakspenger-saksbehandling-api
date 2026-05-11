package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

enum class MeldekortbehandlingStatusDTO {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    AUTOMATISK_BEHANDLET,
    AVBRUTT,
}

fun MeldekortbehandlingStatus.toStatusDTO(): MeldekortbehandlingStatusDTO {
    return when (this) {
        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> MeldekortbehandlingStatusDTO.KLAR_TIL_BEHANDLING
        MeldekortbehandlingStatus.UNDER_BEHANDLING -> MeldekortbehandlingStatusDTO.UNDER_BEHANDLING
        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortbehandlingStatusDTO.KLAR_TIL_BESLUTNING
        MeldekortbehandlingStatus.UNDER_BESLUTNING -> MeldekortbehandlingStatusDTO.UNDER_BESLUTNING
        MeldekortbehandlingStatus.GODKJENT -> MeldekortbehandlingStatusDTO.GODKJENT
        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> MeldekortbehandlingStatusDTO.AUTOMATISK_BEHANDLET
        MeldekortbehandlingStatus.AVBRUTT -> MeldekortbehandlingStatusDTO.AVBRUTT
    }
}
