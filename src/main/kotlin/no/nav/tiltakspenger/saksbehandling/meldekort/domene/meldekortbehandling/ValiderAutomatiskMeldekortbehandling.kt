package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.felles.erHelligdag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * En korrigering fra bruker må behandles før dette klokkeslettet dagen etter at første meldekortbehandling ble iverksatt for å kunne behandles automatisk.
 * Søndager og helligdager hoppes over når vi finner dagen etter, siden økonomisystemet er stengt da.
 * Utbetalingen har én ventedag før den betales ut, så vi antar at den ikke er utbetalt før fristen.
 */
private val FRIST_FOR_AUTOMATISK_KORRIGERING_DAGEN_ETTER: LocalTime = LocalTime.of(17, 0)

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
 * Korrigeringen må i tillegg behandles før kl. 17 dagen etter at første behandling ble iverksatt, slik at utbetalingen ennå ikke er utbetalt.
 * Søndager og helligdager regnes ikke som dagen etter.
 * Vi sjekker ikke utbetalingsstatusen, siden den kun forteller om Oppdrag har tatt imot utbetalingen og ikke om den er utbetalt.
 */
private fun kanKorrigeresAutomatisk(
    behandledeMeldekort: List<Meldekortbehandling.Behandlet>,
    brukersMeldekort: BrukersMeldekort,
    clock: Clock,
): Boolean {
    val førsteBehandling = behandledeMeldekort.singleOrNull() as? MeldekortBehandletAutomatisk ?: return false
    val førsteInnsending = førsteBehandling.brukersMeldekort
    val frist: LocalDateTime = generateSequence(førsteBehandling.iverksattTidspunkt.toLocalDate().plusDays(1)) { it.plusDays(1) }
        .first { it.dayOfWeek != DayOfWeek.SUNDAY && !it.erHelligdag() }
        .atTime(FRIST_FOR_AUTOMATISK_KORRIGERING_DAGEN_ETTER)

    return førsteInnsending.id != brukersMeldekort.id &&
        førsteInnsending.mottatt < brukersMeldekort.mottatt &&
        førsteInnsending.meldeperiode == brukersMeldekort.meldeperiode &&
        nå(clock) < frist
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
