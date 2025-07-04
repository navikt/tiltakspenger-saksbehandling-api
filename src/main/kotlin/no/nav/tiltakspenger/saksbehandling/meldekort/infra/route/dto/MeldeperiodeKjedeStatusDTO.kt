package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

enum class MeldeperiodeKjedeStatusDTO {
    AVVENTER_MELDEKORT,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
    AUTOMATISK_BEHANDLET,
    IKKE_RETT_TIL_TILTAKSPENGER,
    IKKE_KLAR_TIL_BEHANDLING,
    AVBRUTT,
}

fun Sak.toMeldeperiodeKjedeStatusDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock, brukersmeldekort: BrukersMeldekort?): MeldeperiodeKjedeStatusDTO {
    this.hentSisteMeldekortBehandlingForKjede(kjedeId)?.also {
        return when (it.status) {
            MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
            MeldekortBehandlingStatus.UNDER_BEHANDLING -> MeldeperiodeKjedeStatusDTO.UNDER_BEHANDLING
            MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BESLUTNING
            MeldekortBehandlingStatus.UNDER_BESLUTNING -> MeldeperiodeKjedeStatusDTO.UNDER_BESLUTNING
            MeldekortBehandlingStatus.GODKJENT -> MeldeperiodeKjedeStatusDTO.GODKJENT
            MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
            MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET -> MeldeperiodeKjedeStatusDTO.AUTOMATISK_BEHANDLET
            MeldekortBehandlingStatus.AVBRUTT -> MeldeperiodeKjedeStatusDTO.AVBRUTT
        }
    }

    val meldeperiode = this.hentSisteMeldeperiodeForKjede(kjedeId)

    val forrigeKjede = this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)

    val forrigeBehandling by lazy {
        forrigeKjede?.let {
            this.meldekortBehandlinger.behandledeMeldekortPerKjede[it.kjedeId]?.first()
        }
    }

    /** Kan starte behandling dersom perioden er klar til utfylling og forrige behandling er godkjent,
     *  eller dette er første meldeperiode
     *  */
    val kanBehandles =
        meldeperiode.erKlarTilUtfylling(clock) && (forrigeKjede == null || forrigeBehandling?.erAvsluttet == true)

    return when {
        meldeperiode.girIngenDagerRett() -> MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
        brukersmeldekort != null -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
        kanBehandles -> MeldeperiodeKjedeStatusDTO.AVVENTER_MELDEKORT
        else -> MeldeperiodeKjedeStatusDTO.IKKE_KLAR_TIL_BEHANDLING
    }
}
