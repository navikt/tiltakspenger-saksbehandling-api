package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Utleder prioriteringen av en korrigering fra bruker som ikke kunne behandles automatisk, slik at saksbehandler kan rekke å behandle den før utbetalingen for meldeperioden er utbetalt.
 * Korrigeringen prioriteres dersom den ble mottatt før [korrigeringsfrist], som er samme frist som for automatisk korrigering.
 * Er korrigeringen den første etter endret rett, har den ingen frist for automatisk korrigering.
 * Da er det utbetalingen av den forrige versjonen den må rekke, og fristen regnes fra første behandling av kjeden.
 * Det er tidspunktet korrigeringen ble mottatt som teller, slik at den prioriteres også når jobben først gir den opp etter fristen.
 * Viser simuleringen at meldeperioden allerede er utbetalt, er det for sent å rekke utbetalingen, og korrigeringen prioriteres ikke.
 *
 * @param status utfallet av forsøket på automatisk behandling.
 * @param simuleringViserTidligereUtbetaling om simuleringen viste at meldeperioden allerede er utbetalt.
 * Statusen er da [MeldekortBehandletAutomatiskStatus.HAR_TIDLIGERE_UTBETALING], bortsett fra for en korrigering etter endret rett, som kan stoppes av feilutbetaling eller justering i stedet, se [erKorrigeringEtterEndretRett].
 * @return null dersom ingen behandling av meldeperioden er iverksatt, slik at meldekortet ikke er en korrigering, dersom korrigeringen ble mottatt etter fristen, eller dersom meldeperioden allerede er utbetalt.
 */
fun Sak.utledPrioriteringAvKorrigering(
    brukersMeldekort: BrukersMeldekort,
    status: MeldekortBehandletAutomatiskStatus,
    simuleringViserTidligereUtbetaling: Boolean,
): Prioritering? {
    if (status == MeldekortBehandletAutomatiskStatus.HAR_TIDLIGERE_UTBETALING || simuleringViserTidligereUtbetaling) {
        return null
    }
    val behandledeMeldekort = meldekortbehandlinger.hentBehandledeMeldekortForKjede(brukersMeldekort.kjedeId)
    val førsteIverksettelse = behandledeMeldekort
        .mapNotNull { it.iverksattTidspunkt }
        .minOrNull()
        ?: return null
    val frist = brukersMeldekort.korrigeringsfrist(behandledeMeldekort) ?: fristForKorrigering(førsteIverksettelse)

    if (!brukersMeldekort.mottatt.isBefore(frist)) {
        return null
    }

    return Prioritering.create(
        "Korrigert meldekort ble mottatt kort tid etter forrige meldekort for perioden, men kunne ikke behandles automatisk.",
    )
}

/**
 * Prioriteringen til en korrigering fra bruker som venter på en meldekortbehandling av en av [kjedeIder].
 * Brukes for å overføre prioriteringen til meldekortbehandlingen saksbehandler starter, siden raden for meldekortet da forsvinner fra benken.
 * Brukes også for å vise prioriteringen på meldeperiodekjeden i personoversikten.
 *
 * Speiler regelen benken bruker for meldekort fra bruker: det nyeste meldekortet i kjeden som gikk til manuell behandling, vises så lenge det venter på behandling, se [venterPåBehandling].
 */
fun Sak.prioriteringFraKorrigeringSomVenterPåBehandling(kjedeIder: List<MeldeperiodeKjedeId>): Prioritering? =
    kjedeIder.firstNotNullOfOrNull { kjedeId ->
        val sisteTilManuellBehandling = brukersMeldekort
            .filter { it.kjedeId == kjedeId && !it.behandlesAutomatisk && it.behandletAutomatiskStatus != MeldekortBehandletAutomatiskStatus.BEHANDLET }
            .maxByOrNull { it.mottatt }
            ?: return@firstNotNullOfOrNull null

        sisteTilManuellBehandling.prioritering.takeIf { venterPåBehandling(sisteTilManuellBehandling) }
    }
