package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.sak.Sak
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
    this.hentSisteMeldekortBehandlingForKjede(kjedeId)?.also {
        return when (it.status) {
            MeldekortBehandlingStatus.GODKJENT -> MeldeperiodeKjedeStatusDTO.GODKJENT
            MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BESLUTNING
            MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
            MeldekortBehandlingStatus.IKKE_BEHANDLET -> when (it.type) {
                MeldekortBehandlingType.FØRSTE_BEHANDLING -> MeldeperiodeKjedeStatusDTO.UNDER_BEHANDLING
                MeldekortBehandlingType.KORRIGERING -> MeldeperiodeKjedeStatusDTO.UNDER_KORRIGERING
            }
        }
    }

    val meldeperiode = this.hentSisteMeldeperiodeForKjede(kjedeId)

    val forrigeKjede = this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)

    val forrigeBehandlingStatus by lazy {
        forrigeKjede?.let {
            this.meldekortBehandlinger.behandledeMeldekortPerKjede[it.kjedeId]?.first()?.status
        }
    }

    /** Kan starte behandling dersom perioden er klar til utfylling og forrige behandling er godkjent,
     *  eller dette er første meldeperiode
     *  */
    val kanBehandles =
        meldeperiode.erKlarTilUtfylling(clock) && (forrigeKjede == null || forrigeBehandlingStatus == MeldekortBehandlingStatus.GODKJENT)

    return when {
        meldeperiode.helePeriodenErSperret() -> MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
        kanBehandles -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
        else -> MeldeperiodeKjedeStatusDTO.IKKE_KLAR_TIL_BEHANDLING
    }
}
