package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError

/**
 * Feil som gjør at vi ikke klarte å avgjøre en tilgangsvurdering (Left-kanalen).
 *
 * Merk skillet mot [no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering]:
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
}
