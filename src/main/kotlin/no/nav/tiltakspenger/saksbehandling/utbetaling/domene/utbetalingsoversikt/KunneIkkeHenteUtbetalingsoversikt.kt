package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata

sealed interface KunneIkkeHenteUtbetalingsoversikt {
    /** Tjenesten avviste oss, ikke personen det ble spurt om. */
    data class TilgangAvvist(
        val httpKlientError: HttpKlientError.UventetStatus,
    ) : KunneIkkeHenteUtbetalingsoversikt

    /** Kallet ga ikke et brukbart svar: ingen respons, request som ikke ble sendt, eller en status vi ikke kjenner. */
    data class Tjenestefeil(
        val httpKlientError: HttpKlientError,
    ) : KunneIkkeHenteUtbetalingsoversikt

    /** Tjenesten avviste oppslaget for denne personen. */
    data class SakAvvist(
        val httpKlientError: HttpKlientError.UventetStatus,
    ) : KunneIkkeHenteUtbetalingsoversikt

    /** Svaret lot seg ikke lese som json i forventet form. */
    data class UleseligSvar(
        val httpKlientError: HttpKlientError.DeserializationError,
    ) : KunneIkkeHenteUtbetalingsoversikt

    /**
     * Svaret ble lest, men innholdet brøt kontrakten.
     * Kallet lyktes, så varianten har [metadata] i stedet for en [HttpKlientError].
     */
    data class UgyldigInnhold(
        val feil: UtbetalingsoversiktMappingfeil,
        val metadata: HttpKlientMetadata,
    ) : KunneIkkeHenteUtbetalingsoversikt
}

/** [felt] er navnet slik det står i json-svaret fra kilden. */
sealed interface UtbetalingsoversiktMappingfeil {
    val felt: String

    data class PåkrevdFeltMangler(
        override val felt: String,
    ) : UtbetalingsoversiktMappingfeil

    /** Aktørtypen er en annen enn `PERSON`, `ORGANISASJON` og `SAMHANDLER`. */
    data class UkjentAktørtype(
        override val felt: String,
    ) : UtbetalingsoversiktMappingfeil

    /** Aktørtypen er `PERSON`, men identen er ikke et fødselsnummer. */
    data class UgyldigPersonident(
        override val felt: String,
    ) : UtbetalingsoversiktMappingfeil

    /** Periodens `fom` er etter `tom`. */
    data class UgyldigPeriode(
        override val felt: String,
    ) : UtbetalingsoversiktMappingfeil
}
