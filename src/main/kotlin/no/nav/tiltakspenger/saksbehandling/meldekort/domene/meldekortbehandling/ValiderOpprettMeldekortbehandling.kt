package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.validerOpprettManuellMeldekortbehandling(kjedeId: MeldeperiodeKjedeId): Either<ValiderOpprettMeldekortbehandlingFeil, Unit> {
    return validerOpprettBehandlingPåKjede(kjedeId)
}

fun Sak.validerOpprettAutomatiskMeldekortbehandling(brukersMeldekort: BrukersMeldekort): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    validerTilstanderSomIkkeKanPrøvesPåNytt(brukersMeldekort).onLeft { return it.left() }
    validerTilstanderSomKanPrøvesPåNytt(brukersMeldekort).onLeft { return it.left() }
    return Unit.right()
}

/** Validering som gir permanente feil - skal ikke prøves på nytt */
private fun Sak.validerTilstanderSomIkkeKanPrøvesPåNytt(brukersMeldekort: BrukersMeldekort): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    val kjedeId = brukersMeldekort.kjedeId

    if (!brukersMeldekort.behandlesAutomatisk) {
        return MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK.left()
    }

    val behandlingerKnyttetTilKjede = this.meldekortbehandlinger.hentIkkeAvbrutteBehandlingerForKjede(kjedeId)

    if (behandlingerKnyttetTilKjede.isNotEmpty()) {
        return MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET.left()
    }

    if (brukersMeldekort.harRegistrertHelg() && !this.kanSendeInnHelgForMeldekort) {
        return MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG.left()
    }

    if (brukersMeldekort.harForMangeDagerSammenhengendeGodkjentFravær()) {
        return MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR.left()
    }

    val sisteMeldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    if (brukersMeldekort.meldeperiode != sisteMeldeperiode) {
        return MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE.left()
    }

    if (sisteMeldeperiode.ingenDagerGirRett) {
        return MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT.left()
    }

    if (brukersMeldekort.antallDagerRegistrert > sisteMeldeperiode.maksAntallDagerForMeldeperiode) {
        return MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT.left()
    }

    return Unit.right()
}

/** Validering som kan gi midlertidige feil - kan prøves på nytt */
private fun Sak.validerTilstanderSomKanPrøvesPåNytt(brukersMeldekort: BrukersMeldekort): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    val kjedeId = brukersMeldekort.kjedeId

    if (revurderinger.harÅpenRevurdering()) {
        return MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING.left()
    }

    validerOpprettBehandlingPåKjede(kjedeId).onLeft {
        return when (it) {
            ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING -> MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING
            ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_FØRSTE_KJEDE -> MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
            ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE -> MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE
        }.left()
    }

    return Unit.right()
}

private fun Sak.validerOpprettBehandlingPåKjede(kjedeId: MeldeperiodeKjedeId): Either<ValiderOpprettMeldekortbehandlingFeil, Unit> {
    if (this.meldekortbehandlinger.harÅpenBehandling) {
        return ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING.left()
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val erFørsteBehandlingPåSaken = this.meldekortbehandlinger.isEmpty()
    val førsteMeldeperiodeMedRettPåSaken = this.meldeperiodeKjeder
        .meldeperiodeKjederMedRett.firstOrNull()
        ?.hentSisteMeldeperiode()

    if (erFørsteBehandlingPåSaken && førsteMeldeperiodeMedRettPåSaken != null && !meldeperiode.ingenDagerGirRett && førsteMeldeperiodeMedRettPåSaken != meldeperiode) {
        return ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_FØRSTE_KJEDE.left()
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjedeMedRett(kjedeId)?.also { foregåendeMeldeperiodekjede ->
        if (!kjedeHarGodkjentEllerIkkeRettMeldekortbehandling(foregåendeMeldeperiodekjede.kjedeId)) {
            return ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE.left()
        }
    }

    return Unit.right()
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
}
