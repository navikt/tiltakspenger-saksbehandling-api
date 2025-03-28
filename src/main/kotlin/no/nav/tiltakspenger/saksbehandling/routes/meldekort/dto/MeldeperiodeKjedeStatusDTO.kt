package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import java.time.Clock

enum class MeldeperiodeKjedeStatusDTO {
    IKKE_RETT_TIL_TILTAKSPENGER,
    IKKE_KLAR_TIL_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    UNDER_KORRIGERING,
    KLAR_TIL_BESLUTNING,
    GODKJENT,
}

fun Sak.toMeldeperiodeKjedeStatusDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeStatusDTO {
    val meldekortBehandling = this.hentSisteMeldekortBehandlingForKjede(kjedeId)
    val meldeperiode = this.hentSisteMeldeperiodeForKjede(kjedeId)

    return when (meldekortBehandling?.status) {
        MeldekortBehandlingStatus.GODKJENT -> MeldeperiodeKjedeStatusDTO.GODKJENT
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
        MeldekortBehandlingStatus.IKKE_BEHANDLET -> when (meldekortBehandling.type) {
            MeldekortBehandlingType.FØRSTE_BEHANDLING -> MeldeperiodeKjedeStatusDTO.UNDER_BEHANDLING
            MeldekortBehandlingType.KORRIGERING -> MeldeperiodeKjedeStatusDTO.UNDER_KORRIGERING
        }

        null -> when {
            meldeperiode.helePeriodenErSperret() -> MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
            !meldeperiode.erKlarTilUtfylling(clock) -> MeldeperiodeKjedeStatusDTO.IKKE_KLAR_TIL_BEHANDLING
            else -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
        }
    }
}
