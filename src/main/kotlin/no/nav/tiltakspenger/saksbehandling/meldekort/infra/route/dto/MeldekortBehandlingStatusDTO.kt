package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus

enum class MeldekortBehandlingStatusDTO {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    AUTOMATISK_BEHANDLET,
    IKKE_RETT_TIL_TILTAKSPENGER,
    AVBRUTT,
}

fun MeldekortBehandling.toStatusDTO(): MeldekortBehandlingStatusDTO {
    return when (this.status) {
        MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> MeldekortBehandlingStatusDTO.UNDER_BEHANDLING
        MeldekortBehandlingStatus.UNDER_BEHANDLING -> MeldekortBehandlingStatusDTO.UNDER_BEHANDLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatusDTO.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.UNDER_BESLUTNING -> MeldekortBehandlingStatusDTO.UNDER_BESLUTNING
        MeldekortBehandlingStatus.GODKJENT -> MeldekortBehandlingStatusDTO.GODKJENT
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
        MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET -> MeldekortBehandlingStatusDTO.AUTOMATISK_BEHANDLET
        MeldekortBehandlingStatus.AVBRUTT -> MeldekortBehandlingStatusDTO.AVBRUTT
    }
}
