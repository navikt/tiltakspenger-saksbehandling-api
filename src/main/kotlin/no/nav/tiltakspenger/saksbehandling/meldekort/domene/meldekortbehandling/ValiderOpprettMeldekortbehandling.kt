package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak

private val logger = KotlinLogging.logger { }

fun Sak.validerOpprettManuellMeldekortbehandling(kjedeId: MeldeperiodeKjedeId): Either<ValiderOpprettMeldekortbehandlingFeil, Unit> {
    val åpenBehandling = this.meldekortbehandlinger.åpenMeldekortbehandling

    if (åpenBehandling != null) {
        /*
          Det er et gyldig valg å gjenopprette en behandling som har blitt lagt tilbake på samme kjede.
          Denne vil ha status KLAR_TIL_BEHANDLING

          Vi tillater ikke å faktisk opprette en ny behandling dersom det finnes en åpen behandling.
         */
        if (åpenBehandling.kjedeIdLegacy != kjedeId || åpenBehandling.status != MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING) {
            return ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING.left()
        }
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    if (meldeperiode.ingenDagerGirRett) {
        val harMottattMeldekortEtterSisteBehandling = this.kjedeHarUbehandletBrukersMeldekort(kjedeId)

        /* Dersom det finnes et ubehandlet meldekort fra bruker må vi tillate å behandle/avbryte dette meldekortet
          Kan skje dersom vi mottok meldekortet før en stans eller omgjøring fjernet retten til tiltakspenger for en meldeperiode
         */
        if (harMottattMeldekortEtterSisteBehandling) {
            val forrigeKjede = this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)

            // Må være første kjede med et ubehandlet meldekort
            if (forrigeKjede == null ||
                (
                    !kjedeHarUbehandletBrukersMeldekort(forrigeKjede.kjedeId) &&
                        kjedeHarGodkjentEllerIkkeRettMeldekortbehandling(forrigeKjede.kjedeId)
                    )
            ) {
                return Unit.right()
            }
        }

        return ValiderOpprettMeldekortbehandlingFeil.INGEN_DAGER_GIR_RETT.left()
    }

    return validerOpprettBehandlingPåKjede(kjedeId)
}

fun Sak.validerOpprettAutomatiskMeldekortbehandling(brukersMeldekort: BrukersMeldekort): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    val meldekortId = brukersMeldekort.id
    val kjedeId = brukersMeldekort.kjedeId

    if (!brukersMeldekort.behandlesAutomatisk) {
        logger.error { "Brukers meldekort $meldekortId skal ikke behandles automatisk" }
        return MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK.left()
    }

    val behandlingerKnyttetTilKjede = this.meldekortbehandlinger.hentIkkeAvbrutteBehandlingerForKjede(kjedeId)

    if (behandlingerKnyttetTilKjede.isNotEmpty()) {
        logger.error { "Meldeperiodekjeden $kjedeId har allerede minst en behandling. Vi støtter ikke automatisk korrigering fra bruker (meldekort id $meldekortId)" }
        return MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET.left()
    }

    val sisteMeldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    if (sisteMeldeperiode.ingenDagerGirRett) {
        return MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT.left()
    }

    if (brukersMeldekort.meldeperiode != sisteMeldeperiode) {
        logger.error { "Meldeperioden for brukers meldekort må være like siste meldeperiode på kjeden for å kunne behandles (meldekort id $meldekortId)" }
        return MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE.left()
    }

    if (brukersMeldekort.antallDagerRegistrert > sisteMeldeperiode.maksAntallDagerForMeldeperiode) {
        logger.error { "Brukers meldekort $meldekortId har for mange dager registret" }
        return MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT.left()
    }

    if (meldekortbehandlinger.åpenMeldekortbehandling != null) {
        return MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING.left()
    }

    if (revurderinger.harÅpenRevurdering()) {
        return MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING.left()
    }

    validerOpprettBehandlingPåKjede(kjedeId).onLeft {
        return when (it) {
            ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING -> MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING
            ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_FØRSTE_KJEDE -> MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
            ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE -> MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE
            ValiderOpprettMeldekortbehandlingFeil.INGEN_DAGER_GIR_RETT -> MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT
        }.left()
    }

    return Unit.right()
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
        if (!kjedeHarGodkjentEllerIkkeRettMeldekortbehandling(foregåendeMeldeperiodekjede.kjedeId)) {
            return ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE.left()
        }
    }

    return Unit.right()
}

/** @return true dersom det ikke er opprettet eller endret på en meldekortbehandling på kjeden etter siste mottatte meldekort fra bruker */
private fun Sak.kjedeHarUbehandletBrukersMeldekort(kjedeId: MeldeperiodeKjedeId): Boolean {
    val sisteBrukersMeldekort = this.brukersMeldekort.filter { it.kjedeId == kjedeId }.maxByOrNull { it.mottatt }
    val sisteBehandling = this.meldekortbehandlinger.hentSisteMeldekortbehandlingForKjede(kjedeId)

    return sisteBrukersMeldekort != null && (sisteBehandling == null || sisteBehandling.sistEndret < sisteBrukersMeldekort.mottatt)
}

private fun Sak.kjedeHarGodkjentEllerIkkeRettMeldekortbehandling(kjedeId: MeldeperiodeKjedeId): Boolean {
    val harGodkjentBehandling = this.meldekortbehandlinger
        .hentIkkeAvbrutteBehandlingerForKjede(kjedeId)
        .let { behandling ->
            behandling.any { it.erGodkjent }
        }

    val harBehandlingAvbruttUtenRett by lazy {
        this.meldekortbehandlinger.hentAvbrutteBehandlingerForKjede(kjedeId)
            .let { behandling -> behandling.any { it.ingenDagerGirRett } }
    }

    return harGodkjentBehandling || harBehandlingAvbruttUtenRett
}

enum class ValiderOpprettMeldekortbehandlingFeil {
    HAR_ÅPEN_BEHANDLING,
    MÅ_BEHANDLE_FØRSTE_KJEDE,
    MÅ_BEHANDLE_NESTE_KJEDE,
    INGEN_DAGER_GIR_RETT,
}
