package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
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
    KORRIGERT_MELDEKORT,
}

fun Sak.toMeldeperiodeKjedeStatusDTO(
    kjedeId: MeldeperiodeKjedeId,
    clock: Clock,
): MeldeperiodeKjedeStatusDTO {
    val brukersMeldekort = this.brukersMeldekort.filter { it.kjedeId == kjedeId }
    val sisteInnsendteMeldekort = brukersMeldekort.maxByOrNull { it.mottatt }
    val sisteMeldekortBehandling =
        this.meldekortbehandlinger.filter { it.kjedeId == kjedeId }.maxByOrNull { it.opprettet }

    val harMottattMeldekortEtterSisteBehandling =
        sisteInnsendteMeldekort != null && (sisteMeldekortBehandling == null || sisteInnsendteMeldekort.mottatt > sisteMeldekortBehandling.opprettet)

    if (harMottattMeldekortEtterSisteBehandling) {
        return if (brukersMeldekort.size == 1) {
            MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
        } else {
            MeldeperiodeKjedeStatusDTO.KORRIGERT_MELDEKORT
        }
    }

    if (sisteMeldekortBehandling != null) {
        return sisteMeldekortBehandling.status.tilMeldeperiodeKjedeStatusDTO()
    }

    val meldeperiode = this.hentSisteMeldeperiodeForKjede(kjedeId)

    if (meldeperiode.girIngenDagerRett()) {
        return MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
    }

    val forrigeKjede = this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)

    val forrigeBehandling by lazy {
        forrigeKjede?.let {
            this.meldekortbehandlinger.behandledeMeldekortPerKjede[it.kjedeId]?.first()
        }
    }

    /** Kan starte behandling dersom perioden er klar til utfylling og forrige behandling er godkjent,
     *  eller dette er første meldeperiode
     */
    val kanBehandles =
        meldeperiode.erKlarTilUtfylling(clock) && (forrigeKjede == null || forrigeKjede.siste.ingenDagerGirRett || forrigeBehandling?.erGodkjentEllerIkkeRett == true)

    if (kanBehandles) {
        return MeldeperiodeKjedeStatusDTO.AVVENTER_MELDEKORT
    }

    return MeldeperiodeKjedeStatusDTO.IKKE_KLAR_TIL_BEHANDLING
}

fun MeldekortBehandlingStatus.tilMeldeperiodeKjedeStatusDTO(): MeldeperiodeKjedeStatusDTO {
    return when (this) {
        MeldekortBehandlingStatus.UNDER_BEHANDLING -> MeldeperiodeKjedeStatusDTO.UNDER_BEHANDLING
        MeldekortBehandlingStatus.UNDER_BESLUTNING -> MeldeperiodeKjedeStatusDTO.UNDER_BESLUTNING
        MeldekortBehandlingStatus.GODKJENT -> MeldeperiodeKjedeStatusDTO.GODKJENT
        MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET -> MeldeperiodeKjedeStatusDTO.AUTOMATISK_BEHANDLET
        MeldekortBehandlingStatus.AVBRUTT -> MeldeperiodeKjedeStatusDTO.AVBRUTT
        MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeKjedeStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
    }
}
