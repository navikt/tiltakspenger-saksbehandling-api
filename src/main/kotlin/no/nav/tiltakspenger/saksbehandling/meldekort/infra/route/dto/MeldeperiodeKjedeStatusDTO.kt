package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
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
    VENTER_AUTOMATISK_BEHANDLING,
}

fun Sak.toMeldeperiodeKjedeStatusDTO(
    kjedeId: MeldeperiodeKjedeId,
    clock: Clock,
): MeldeperiodeKjedeStatusDTO {
    val brukersMeldekort = this.brukersMeldekort.filter { it.kjedeId == kjedeId }
    val sisteInnsendteMeldekort = brukersMeldekort.maxByOrNull { it.mottatt }
    val sisteMeldekortbehandling =
        this.meldekortbehandlinger.filter { it.kjedeId == kjedeId }.maxByOrNull { it.opprettet }

    val harMottattMeldekortEtterSisteBehandling =
        sisteInnsendteMeldekort != null && (sisteMeldekortbehandling == null || sisteInnsendteMeldekort.mottatt > sisteMeldekortbehandling.sistEndret)

    if (harMottattMeldekortEtterSisteBehandling) {
        return if (brukersMeldekort.size > 1) {
            MeldeperiodeKjedeStatusDTO.KORRIGERT_MELDEKORT
        } else if (sisteInnsendteMeldekort.behandletAutomatiskStatus == MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING) {
            MeldeperiodeKjedeStatusDTO.VENTER_AUTOMATISK_BEHANDLING
        } else {
            MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING
        }
    }

    if (sisteMeldekortbehandling != null) {
        return sisteMeldekortbehandling.status.tilMeldeperiodeKjedeStatusDTO()
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

fun MeldekortbehandlingStatus.tilMeldeperiodeKjedeStatusDTO(): MeldeperiodeKjedeStatusDTO {
    return when (this) {
        MeldekortbehandlingStatus.UNDER_BEHANDLING -> MeldeperiodeKjedeStatusDTO.UNDER_BEHANDLING

        MeldekortbehandlingStatus.UNDER_BESLUTNING -> MeldeperiodeKjedeStatusDTO.UNDER_BESLUTNING

        MeldekortbehandlingStatus.GODKJENT -> MeldeperiodeKjedeStatusDTO.GODKJENT

        MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> MeldeperiodeKjedeStatusDTO.AUTOMATISK_BEHANDLET

        MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BEHANDLING

        MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> MeldeperiodeKjedeStatusDTO.KLAR_TIL_BESLUTNING

        MeldekortbehandlingStatus.AVBRUTT -> MeldeperiodeKjedeStatusDTO.AVBRUTT

        // Vi skal ikke utlede status på meldeperiodekjeden ut fra om det ikke var rett ved forrige behandling
        // Dette skal kun bestemmes av nyeste meldeperiode
        MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodeKjedeStatusDTO.AVBRUTT
    }
}
