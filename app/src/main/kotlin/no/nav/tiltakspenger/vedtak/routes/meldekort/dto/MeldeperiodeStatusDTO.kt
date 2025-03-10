package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.vedtak.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Sak

enum class MeldeperiodeStatusDTO {
    IKKE_RETT_TIL_TILTAKSPENGER,
    IKKE_KLAR_TIL_UTFYLLING,
    VENTER_PÅ_UTFYLLING,
    KLAR_TIL_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
}

fun Sak.toMeldeperiodeStatusDTO(meldeperiode: Meldeperiode): MeldeperiodeStatusDTO {
    val meldekortBehandling = this.meldekortBehandlinger.find { meldekortBehandling ->
        meldekortBehandling.meldeperiode.id == meldeperiode.id
    }
    val brukersMeldekort = this.brukersMeldekort.findLast { brukersMeldekort ->
        brukersMeldekort.meldeperiode.id == meldeperiode.id
    }

    return when (meldekortBehandling?.status) {
        MeldekortBehandlingStatus.GODKJENT -> MeldeperiodeStatusDTO.GODKJENT
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeStatusDTO.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.IKKE_BEHANDLET -> MeldeperiodeStatusDTO.KLAR_TIL_BEHANDLING
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
        null -> when {
            meldeperiode.helePeriodenErSperret() -> MeldeperiodeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
            !meldeperiode.erKlarTilUtfylling() -> MeldeperiodeStatusDTO.IKKE_KLAR_TIL_UTFYLLING
            brukersMeldekort == null -> MeldeperiodeStatusDTO.VENTER_PÅ_UTFYLLING
            else -> MeldeperiodeStatusDTO.KLAR_TIL_BEHANDLING
        }
    }
}
