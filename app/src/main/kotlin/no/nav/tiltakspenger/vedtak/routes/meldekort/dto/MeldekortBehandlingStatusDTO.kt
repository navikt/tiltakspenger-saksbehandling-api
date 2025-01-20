package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus

enum class MeldekortBehandlingStatusDTO {
    IKKE_KLAR_TIL_UTFYLLING,
    KLAR_TIL_UTFYLLING,
    UTFYLT,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun MeldekortBehandling.toMeldekortstatusDTO(): MeldekortBehandlingStatusDTO {
    return when (val m = this) {
        is MeldekortBehandling.IkkeUtfyltMeldekort -> if (m.erKlarTilUtfylling()) MeldekortBehandlingStatusDTO.KLAR_TIL_UTFYLLING else MeldekortBehandlingStatusDTO.IKKE_KLAR_TIL_UTFYLLING
        is MeldekortBehandling.UtfyltMeldekort -> when (this.status) {
            MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatusDTO.KLAR_TIL_BESLUTNING
            MeldekortBehandlingStatus.GODKJENT -> MeldekortBehandlingStatusDTO.GODKJENT
            MeldekortBehandlingStatus.IKKE_BEHANDLET -> throw IllegalStateException("Utfylt meldekort kan ikke ha status IKKE_UTFYLT")
            MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortBehandlingStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
        }
    }
}
