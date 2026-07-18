package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError

/**
 * Feil ved simulering mot helved.
 * Alle variantene bærer den underliggende [HttpKlientError]-en; den er kun ment for feillogging i kallende service.
 */
sealed interface KunneIkkeSimulere {
    data class UkjentFeil(val feil: HttpKlientError) : KunneIkkeSimulere

    /**
     * OS har åpningstider.
     * Typisk mandag til fredag fra 6 til 21.
     * Men det hender den er stengt på helligdager og vedlikeholdsdager også.
     */
    data class Stengt(val feil: HttpKlientError) : KunneIkkeSimulere

    data class Timeout(val feil: HttpKlientError) : KunneIkkeSimulere
}
