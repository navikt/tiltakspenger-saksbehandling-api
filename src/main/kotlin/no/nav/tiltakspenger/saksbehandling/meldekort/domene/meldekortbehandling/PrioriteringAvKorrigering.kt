package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider

/**
 * Utleder prioriteringen av en korrigering fra bruker som ikke kunne behandles automatisk, slik at saksbehandler kan rekke å behandle den før utbetalingen for meldeperioden er utbetalt.
 * Korrigeringen prioriteres dersom den ble mottatt innen utgangen av neste virkedag etter at siste behandling av meldeperioden ble iverksatt.
 * Lørdager, søndager og helligdager er ikke virkedager, se [Åpningstider.nesteÅpneDag].
 * Økonomisystemet kan være åpent på lørdager, men er det ikke nødvendigvis.
 * Det er tidspunktet korrigeringen ble mottatt som teller, slik at den prioriteres også når jobben først gir den opp etter fristen.
 *
 * Fristen er romsligere enn fristen for automatisk korrigering i [validerAutomatiskMeldekortbehandling].
 * Den automatiske korrigeringen må være sikker på at utbetalingen ikke er utbetalt, mens en prioritering bare gir saksbehandler en mulighet til å rekke det.
 *
 * @return null dersom ingen behandling av meldeperioden er iverksatt, slik at meldekortet ikke er en korrigering, eller dersom korrigeringen ble mottatt etter fristen.
 */
fun Sak.utledPrioriteringAvKorrigering(brukersMeldekort: BrukersMeldekort): Prioritering? {
    val sisteIverksettelse = meldekortbehandlinger.behandledeMeldekortPerKjede[brukersMeldekort.kjedeId]
        .orEmpty()
        .mapNotNull { it.iverksattTidspunkt }
        .maxOrNull()
        ?: return null
    val frist = Åpningstider.nesteÅpneDag(sisteIverksettelse.toLocalDate())

    if (brukersMeldekort.mottatt.toLocalDate().isAfter(frist)) {
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
 * Speiler regelen benken bruker for meldekort fra bruker: det nyeste meldekortet i kjeden som gikk til manuell behandling, venter så lenge ingen meldekortbehandling av kjeden er endret etter at det ble mottatt.
 */
fun Sak.prioriteringFraKorrigeringSomVenterPåBehandling(kjedeIder: List<MeldeperiodeKjedeId>): Prioritering? =
    kjedeIder.firstNotNullOfOrNull { kjedeId ->
        val sisteTilManuellBehandling = brukersMeldekort
            .filter { it.kjedeId == kjedeId && !it.behandlesAutomatisk && it.behandletAutomatiskStatus != MeldekortBehandletAutomatiskStatus.BEHANDLET }
            .maxByOrNull { it.mottatt }
            ?: return@firstNotNullOfOrNull null
        val erTattStillingTil =
            meldekortbehandlinger.any { kjedeId in it.kjedeIder && !it.sistEndret.isBefore(sisteTilManuellBehandling.mottatt) }

        sisteTilManuellBehandling.prioritering.takeUnless { erTattStillingTil }
    }
