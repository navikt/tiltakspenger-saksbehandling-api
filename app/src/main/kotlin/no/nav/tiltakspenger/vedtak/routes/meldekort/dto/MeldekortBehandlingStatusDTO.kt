package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus

enum class MeldekortBehandlingStatusDTO {
    KLAR_TIL_UTFYLLING,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun MeldekortBehandling.toStatusDTO(): MeldekortBehandlingStatusDTO {
    return when (this.status) {
        MeldekortBehandlingStatus.IKKE_BEHANDLET -> MeldekortBehandlingStatusDTO.KLAR_TIL_UTFYLLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatusDTO.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.GODKJENT -> MeldekortBehandlingStatusDTO.GODKJENT
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
    }
}
