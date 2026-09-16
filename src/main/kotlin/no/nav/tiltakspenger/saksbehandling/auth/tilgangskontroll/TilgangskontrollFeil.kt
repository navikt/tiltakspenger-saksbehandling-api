package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata

/**
 * Feil som gjør at vi ikke klarte å avgjøre en tilgangsvurdering (Left-kanalen).
 *
 * Merk skillet mot [Tilgangsvurdering]:
 * en `Avvist`-vurdering er et gyldig forretningsutfall (et definitivt «nei») og hører hjemme på Right.
 * Denne typen brukes kun når vi ikke fikk noen vurdering i det hele tatt.
 */
sealed interface TilgangskontrollFeil {
    /** Bulk-oppslaget ble avvist fordi vi ba om tilgang for flere enn maksgrensen (HTTP 413). */
    data object ForMangeIdenter : TilgangskontrollFeil

    /**
     * Uventet feil i kallet mot tilgangsmaskinen (transport, auth, uventet status, deserialisering).
     *
     * [underliggende] beholdes utelukkende for logging via [no.nav.tiltakspenger.libs.httpklient.loggFeil].
     * Ikke ta domenebeslutninger basert på innholdet her.
     */
    data class Uventet(val underliggende: HttpKlientError) : TilgangskontrollFeil

    /**
     * Tilgangsmaskinen svarte, men svaret lot seg ikke tolke.
     *
     * [beskrivelse] er vår egen, PII-frie forklaring på hva som var galt, og er trygg i vanlig logg.
     * [metadata] er kun for logging og bærer den rå responsen til sikkerloggen.
     */
    data class UgyldigSvar(val beskrivelse: String, val metadata: HttpKlientMetadata) : TilgangskontrollFeil
}
