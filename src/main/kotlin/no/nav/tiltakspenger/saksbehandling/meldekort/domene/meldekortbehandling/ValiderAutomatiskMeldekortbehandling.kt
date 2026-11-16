package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

/**
 * Antall dager etter at førstegangsinnsendingen ble behandlet automatisk, hvor en korrigering fra bruker også kan behandles automatisk.
 * Utbetalingen har én ventedag før den betales ut, så vi antar at den ikke er utbetalt før dagen etter behandlingen er omme.
 */
private const val DAGER_ETTER_FØRSTE_BEHANDLING_FOR_AUTOMATISK_KORRIGERING = 1L

fun Sak.validerAutomatiskMeldekortbehandling(
    brukersMeldekort: BrukersMeldekort,
    clock: Clock,
): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    validerTilstanderSomIkkeKanPrøvesPåNytt(brukersMeldekort, clock).onLeft { return it.left() }
    validerTilstanderSomKanPrøvesPåNytt(brukersMeldekort).onLeft { return it.left() }
    return Unit.right()
}

/** Validering som gir permanente feil - skal ikke prøves på nytt */
private fun Sak.validerTilstanderSomIkkeKanPrøvesPåNytt(
    brukersMeldekort: BrukersMeldekort,
    clock: Clock,
): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    val kjedeId = brukersMeldekort.kjedeId

    if (!brukersMeldekort.behandlesAutomatisk) {
        return MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK.left()
    }

    val behandledeMeldekort = this.meldekortbehandlinger.behandledeMeldekortPerKjede[kjedeId] ?: emptyList()

    if (behandledeMeldekort.isNotEmpty() && !kanKorrigeresAutomatisk(behandledeMeldekort, brukersMeldekort, clock)) {
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

/**
 * En korrigering fra bruker kan behandles automatisk dersom kjeden kun har én behandling, og den ble behandlet automatisk på samme versjon av meldeperioden.
 * Korrigeringen må i tillegg behandles senest dagen etter førstegangsinnsendingen ble behandlet, slik at utbetalingen ennå ikke er utbetalt.
 * Vi sjekker ikke utbetalingsstatusen, siden den kun forteller om Oppdrag har tatt imot utbetalingen og ikke om den er utbetalt.
 */
private fun kanKorrigeresAutomatisk(
    behandledeMeldekort: List<Meldekortbehandling.Behandlet>,
    brukersMeldekort: BrukersMeldekort,
    clock: Clock,
): Boolean {
    val førsteBehandling = behandledeMeldekort.singleOrNull() as? MeldekortBehandletAutomatisk ?: return false
    val førsteInnsending = førsteBehandling.brukersMeldekort
    val sisteDagForAutomatiskKorrigering = førsteBehandling.iverksattTidspunkt.toLocalDate()
        .plusDays(DAGER_ETTER_FØRSTE_BEHANDLING_FOR_AUTOMATISK_KORRIGERING)

    return førsteInnsending.id != brukersMeldekort.id &&
        førsteInnsending.mottatt < brukersMeldekort.mottatt &&
        førsteInnsending.meldeperiode == brukersMeldekort.meldeperiode &&
        !nå(clock).toLocalDate().isAfter(sisteDagForAutomatiskKorrigering)
}

/** Validering som kan gi midlertidige feil - kan prøves på nytt */
private fun Sak.validerTilstanderSomKanPrøvesPåNytt(brukersMeldekort: BrukersMeldekort): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    val kjedeId = brukersMeldekort.kjedeId

    // Meldekort skal ikke behandles automatisk så lenge det finnes en åpen revurdering som kan fjerne retten til TP.
    if (revurderinger.harÅpenRevurderingSomKanFjerneRett()) {
        return MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING.left()
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val erFørsteBehandlingPåSaken = this.meldekortbehandlinger.isEmpty()
    val førsteMeldeperiodeMedRettPåSaken = this.meldeperiodeKjeder
        .meldeperiodeKjederMedRett.firstOrNull()
        ?.hentSisteMeldeperiode()

    if (erFørsteBehandlingPåSaken && førsteMeldeperiodeMedRettPåSaken != null && !meldeperiode.ingenDagerGirRett && førsteMeldeperiodeMedRettPåSaken != meldeperiode) {
        return MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE.left()
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjedeMedRett(kjedeId)?.also { foregåendeMeldeperiodekjede ->
        if (!kjedeHarGodkjentEllerIkkeRettMeldekortbehandling(foregåendeMeldeperiodekjede.kjedeId)) {
            return MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE.left()
        }
    }

    if (this.meldekortbehandlinger.hentÅpenBehandlingForKjede(kjedeId) != null) {
        return MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING.left()
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
