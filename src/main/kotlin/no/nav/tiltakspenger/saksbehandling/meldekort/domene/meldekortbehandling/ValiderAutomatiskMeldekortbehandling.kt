package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider
import java.time.Clock
import java.time.LocalDateTime

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

    if (behandledeMeldekort.isNotEmpty()) {
        brukersMeldekort.validerKanKorrigeresAutomatisk(behandledeMeldekort, clock).onLeft { return it.left() }
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
 * En korrigering fra bruker kan behandles automatisk dersom den behandles før [fristForKorrigering], regnet fra første iverksatte behandling av kjeden.
 * Den må i tillegg være mottatt etter innsendingene som er behandlet automatisk, og alle tidligere behandlinger av kjeden må være automatiske.
 * Sjekkene gjøres i den rekkefølgen, slik at statusen sier om det er for sent før den sier noe om de tidligere behandlingene.
 * Det er ikke et krav at de tidligere behandlingene gjaldt samme versjon av meldeperioden.
 * Fristen sparer oss for simuleringer som uansett ville vist at utbetalingen er utbetalt.
 * Om utbetalingen faktisk er utbetalt, avgjøres av simuleringen, se [no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering.harTidligereUtbetaling].
 * Vi sjekker ikke utbetalingsstatusen, siden den kun forteller om Oppdrag har tatt imot utbetalingen og ikke om den er utbetalt.
 * Fristen gjelder ikke en korrigering etter endret rett, se [erKorrigeringEtterEndretRett].
 */
private fun BrukersMeldekort.validerKanKorrigeresAutomatisk(
    behandledeMeldekort: List<Meldekortbehandling.Behandlet>,
    clock: Clock,
): Either<MeldekortBehandletAutomatiskStatus, Unit> {
    val førsteIverksettelse = behandledeMeldekort.mapNotNull { it.iverksattTidspunkt }.minOrNull()
    if (!erKorrigeringEtterEndretRett(behandledeMeldekort) &&
        førsteIverksettelse != null &&
        !nå(clock).isBefore(fristForKorrigering(førsteIverksettelse))
    ) {
        return MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE.left()
    }

    val automatiskeBehandlinger = behandledeMeldekort.filterIsInstance<MeldekortBehandletAutomatisk>()
    // Fanger også at korrigeringen selv er behandlet, siden den da har samme mottatt-tidspunkt som den behandlede innsendingen.
    if (automatiskeBehandlinger.any { it.brukersMeldekort.mottatt >= this.mottatt }) {
        return MeldekortBehandletAutomatiskStatus.NYERE_INNSENDING_ER_BEHANDLET.left()
    }

    if (automatiskeBehandlinger.size < behandledeMeldekort.size) {
        return MeldekortBehandletAutomatiskStatus.TIDLIGERE_BEHANDLET_MANUELT.left()
    }

    return Unit.right()
}

/**
 * Om meldekortet fra bruker korrigerer en meldeperiode som har fått en nyere versjon med flere dager med rett, eller nytt maks antall dager, siden forrige behandling av kjeden.
 * Typisk fordi en revurdering har endret retten etter at meldeperioden ble behandlet, slik at bruker må kunne melde på nytt.
 * En slik korrigering kan behandles automatisk etter [fristForKorrigering], og selv om simuleringen viser at meldeperioden allerede er utbetalt.
 * De andre kravene til automatisk korrigering gjelder fortsatt, blant annet at simuleringen ikke kan vise feilutbetaling eller justering.
 *
 * @param behandledeMeldekort behandlingene av kjeden, sortert i behandlingsrekkefølge, se [Meldekortbehandlinger.behandledeMeldekortPerKjede].
 * @return false dersom kjeden ikke er behandlet før, slik at meldekortet ikke er en korrigering.
 */
fun BrukersMeldekort.erKorrigeringEtterEndretRett(behandledeMeldekort: List<Meldekortbehandling.Behandlet>): Boolean {
    val forrigeMeldeperiode = behandledeMeldekort.lastOrNull()
        ?.meldeperioder
        ?.single { it.kjedeId == kjedeId }
        ?.meldeperiode
        ?: return false

    return meldeperiode.versjon > forrigeMeldeperiode.versjon &&
        meldeperiode.harFlereDagerMedRettEllerNyttAntallDager(forrigeMeldeperiode)
}

/**
 * Fristen for å korrigere en meldeperiode før utbetalingen kan ha gått gjennom, gitt tidspunktet første behandling av kjeden ble iverksatt.
 * Fristen er 10 minutter før stengetid neste virkedag, se [Åpningstider.senesteÅpneTidspunkt].
 * Lørdager regnes ikke som virkedager, siden økonomisystemet kan være stengt da, se [Åpningstider.nesteÅpneDag].
 * Økonomisystemet er likevel av og til åpent på lørdager.
 * Det gir en romsligere frist, som er trygt siden simuleringen fanger opp om meldeperioden likevel er utbetalt på lørdagen.
 */
fun fristForKorrigering(førsteIverksettelse: LocalDateTime): LocalDateTime =
    Åpningstider.senesteÅpneTidspunkt(Åpningstider.nesteÅpneDag(førsteIverksettelse.toLocalDate()))

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
