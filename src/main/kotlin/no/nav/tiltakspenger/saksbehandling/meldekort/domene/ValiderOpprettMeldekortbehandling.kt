package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.validerOpprettManuellMeldekortbehandling(kjedeId: MeldeperiodeKjedeId): Either<ValiderOpprettMeldekortbehandlingFeil, Unit> {
    val åpenBehandling = this.meldekortbehandlinger.åpenMeldekortBehandling

    if (åpenBehandling != null) {
        /**
         *  Det er et gyldig valg å gjenopprette en behandling som har blitt lagt tilbake på samme kjede.
         *  Denne vil ha status KLAR_TIL_BEHANDLING
         *
         *  Vi tillater ikke å faktisk opprette en ny behandling dersom det finnes en åpen behandling.
         *  */
        if (åpenBehandling.kjedeId != kjedeId || åpenBehandling.status != MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING) {
            return ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING.left()
        }
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    if (meldeperiode.ingenDagerGirRett) {
        val harMottattMeldekortEtterSisteBehandling = this.kjedeHarUbehandletBrukersMeldekort(kjedeId)

        /** Dersom det finnes et ubehandlet meldekort fra bruker må vi tillate å behandle/avbryte dette meldekortet
         *  Kan skje dersom vi mottok meldekortet før en stans eller omgjøring fjernet retten til tiltakspenger for en meldeperiode
         * */
        if (harMottattMeldekortEtterSisteBehandling) {
            val forrigeKjede = this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)

            // Må være første kjede med et ubehandlet meldekort
            if (forrigeKjede == null ||
                (
                    !kjedeHarUbehandletBrukersMeldekort(forrigeKjede.kjedeId) &&
                        kjedeHarGodkjentMeldekortbehandling(forrigeKjede.kjedeId)
                    )
            ) {
                return Unit.right()
            }
        }

        return ValiderOpprettMeldekortbehandlingFeil.INGEN_DAGER_GIR_RETT.left()
    }

    return validerOpprettBehandlingPåKjede(kjedeId)
}

fun Sak.validerOpprettAutomatiskMeldekortbehandling(kjedeId: MeldeperiodeKjedeId): Either<ValiderOpprettMeldekortbehandlingFeil, Unit> {
    val åpenBehandling = this.meldekortbehandlinger.åpenMeldekortBehandling

    if (åpenBehandling != null) {
        return ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING.left()
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    if (meldeperiode.ingenDagerGirRett) {
        return ValiderOpprettMeldekortbehandlingFeil.INGEN_DAGER_GIR_RETT.left()
    }

    return validerOpprettBehandlingPåKjede(kjedeId)
}

private fun Sak.validerOpprettBehandlingPåKjede(kjedeId: MeldeperiodeKjedeId): Either<ValiderOpprettMeldekortbehandlingFeil, Unit> {
    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val erFørsteBehandlingPåSaken = this.meldekortbehandlinger.isEmpty()
    val erFørsteMeldeperiodeMedRettPåSaken = meldeperiode == this.meldeperiodeKjeder
        .meldeperiodeKjederMedRett.first()
        .hentSisteMeldeperiode()

    if (erFørsteBehandlingPåSaken && !erFørsteMeldeperiodeMedRettPåSaken) {
        return ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_FØRSTE_KJEDE.left()
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjedeMedRett(kjedeId)?.also { foregåendeMeldeperiodekjede ->
        if (!kjedeHarGodkjentMeldekortbehandling(foregåendeMeldeperiodekjede.kjedeId)) {
            return ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE.left()
        }
    }

    return Unit.right()
}

/** @return true dersom det ikke er opprettet eller endret på en meldekortbehandling på kjeden etter siste mottatte meldekort fra bruker */
private fun Sak.kjedeHarUbehandletBrukersMeldekort(kjedeId: MeldeperiodeKjedeId): Boolean {
    val sisteBrukersMeldekort = this.brukersMeldekort.filter { it.kjedeId == kjedeId }.maxByOrNull { it.mottatt }
    val sisteBehandling = this.meldekortbehandlinger.hentSisteMeldekortBehandlingForKjede(kjedeId)

    return sisteBrukersMeldekort != null && (sisteBehandling == null || sisteBehandling.sistEndret < sisteBrukersMeldekort.mottatt)
}

private fun Sak.kjedeHarGodkjentMeldekortbehandling(kjedeId: MeldeperiodeKjedeId): Boolean {
    return this.meldekortbehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)
        .let { behandling ->
            behandling.any { it.status == GODKJENT || it.status == AUTOMATISK_BEHANDLET }
        }
}

enum class ValiderOpprettMeldekortbehandlingFeil {
    HAR_ÅPEN_BEHANDLING,
    MÅ_BEHANDLE_FØRSTE_KJEDE,
    MÅ_BEHANDLE_NESTE_KJEDE,
    INGEN_DAGER_GIR_RETT,
}
