package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

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
        meldekortBehandling.meldeperiode.hendelseId == meldeperiode.hendelseId
    }
    val brukersMeldekort = this.brukersMeldekort.findLast { brukersMeldekort ->
        brukersMeldekort.meldeperiode.hendelseId == meldeperiode.hendelseId
    }

    return when (meldekortBehandling?.status) {
        MeldekortBehandlingStatus.GODKJENT -> MeldeperiodeStatusDTO.GODKJENT
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeStatusDTO.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.IKKE_BEHANDLET -> MeldeperiodeStatusDTO.KLAR_TIL_BEHANDLING
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
        null -> when {
            meldeperiode.periode.fraOgMed > nå().toLocalDate() -> MeldeperiodeStatusDTO.IKKE_KLAR_TIL_UTFYLLING
            brukersMeldekort == null -> MeldeperiodeStatusDTO.VENTER_PÅ_UTFYLLING
            else -> MeldeperiodeStatusDTO.KLAR_TIL_BEHANDLING
        }
    }
}
