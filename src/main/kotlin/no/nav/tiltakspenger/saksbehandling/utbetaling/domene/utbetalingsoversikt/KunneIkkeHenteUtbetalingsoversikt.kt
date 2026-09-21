package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata

sealed interface KunneIkkeHenteUtbetalingsoversikt {
    val feiltype: Oppslagsfeiltype
    val metadata: HttpKlientMetadata

    /** Tjenesten avviste oss, ikke personen det ble spurt om. */
    data class TilgangAvvist(
        val httpKlientError: HttpKlientError.UventetStatus,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.TILGANG_AVVIST
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
    }

    /** Kallet ga ikke et brukbart svar: ingen respons, request som ikke ble sendt, eller en status vi ikke kjenner. */
    data class Tjenestefeil(
        val httpKlientError: HttpKlientError,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.TJENESTEFEIL
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
    }

    /** Tjenesten avviste oppslaget for denne personen. */
    data class SakAvvist(
        val httpKlientError: HttpKlientError.UventetStatus,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.SAK_AVVIST
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
    }

    /** Svaret lot seg ikke lese som json i forventet form. */
    data class UleseligSvar(
        val httpKlientError: HttpKlientError.DeserializationError,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.ULESELIG_SVAR
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
    }

    /**
     * Svaret ble lest, men innholdet brøt kontrakten.
     * Kallet lyktes, så varianten har [metadata] i stedet for en [HttpKlientError].
     */
    data class UgyldigInnhold(
        val feil: UtbetalingsoversiktMappingfeil,
        override val metadata: HttpKlientMetadata,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.UGYLDIG_INNHOLD
    }
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
