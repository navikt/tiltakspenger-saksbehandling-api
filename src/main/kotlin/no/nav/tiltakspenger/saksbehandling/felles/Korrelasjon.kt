package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.CorrelationId

/**
 * Gir hvert element en jobb behandler, for eksempel ett vedtak eller én utbetaling, sin egen correlation-id.
 * Id-en gis til [medNyCorrelationId]-blokken, slik at den kan sendes videre i kall til andre tjenester.
 * Implementasjonen knytter loggene fra blokken til samme id, så ett element kan følges fra logglinje til nedstrøms kall.
 * Id-en gjelder bare inne i blokken, slik at den ikke lekker til neste element, neste jobb eller neste test.
 */
interface Korrelasjon {
    suspend fun <T> medNyCorrelationId(block: suspend (CorrelationId) -> T): T
}
