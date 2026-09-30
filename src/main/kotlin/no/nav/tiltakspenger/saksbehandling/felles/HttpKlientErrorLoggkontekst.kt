package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.throwableOrNull

/**
 * Loggkonteksten til en feil fra `httpklient`, for feil som logges én gang av route-laget gjennom [Loggbar].
 * Teksten følger `HttpKlientError.loggFeil`: feilart, endepunkt, status, forsøk og varighet.
 * Den underliggende feilen legges bare i [sikkerloggkontekst], fordi meldingen kan bære deler av responsen.
 */
// TODO: Flytt til tiltakspenger-libs ved siden av HttpKlientError.loggFeil når Loggbar flyttes dit.
fun HttpKlientError.loggkontekst(operasjon: String): Loggkontekst {
    val status = metadata.statusCode?.let { "status: $it, " } ?: ""
    return Loggkontekst(
        "Feil ved $operasjon. $beskrivelse mot ${metadata.endepunkt}. ${status}forsøk: ${metadata.attempts}, brukt: ${metadata.totalDuration}",
    )
}

/** Rå request og respons, som kan inneholde fødselsnumre, og den underliggende feilen. */
fun HttpKlientError.sikkerloggkontekst(operasjon: String): Loggkontekst = Loggkontekst(
    "Feil ved $operasjon. Request: ${metadata.rawRequestString}. Response: ${metadata.rawResponseString}",
    throwableOrNull(),
)
