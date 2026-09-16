package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering

/**
 * Eies av [no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinHttpClient] og skal ikke brukes andre plasser.
 */
data class AvvistTilgangResponseDto(
    val type: String,
    /**
     * Avvisningskoden Tilgangsmaskinen svarte med, rå slik den står i svaret.
     * [tilTilgangsvurderingAvvistÅrsak] oversetter den til domenets årsak.
     */
    val title: String,
    val status: Int,
    val navIdent: String,
    val begrunnelse: String,
) {
    /**
     * Mappingen kan ikke feile: en kode vi ikke kjenner, blir UKJENT.
     * Den rå koden følger med i metadata, slik at loggen viser hva Tilgangsmaskinen faktisk svarte.
     *
     * [fnr] er identen vi spurte om tilgang til.
     * Tilgangsmaskinen svarer med personens gjeldende ident i `brukerIdent`, som vi antar er den samme, og som vi derfor ikke leser.
     */
    fun tilAvvistTilgangsvurdering(fnr: Fnr): Tilgangsvurdering.Avvist = Tilgangsvurdering.Avvist(
        årsak = tilTilgangsvurderingAvvistÅrsak(title),
        begrunnelse = begrunnelse,
        metadata = AvvistMetadata(
            type = type,
            avvisningskode = title,
            navIdent = navIdent,
            brukerIdent = fnr,
        ),
    )
}
