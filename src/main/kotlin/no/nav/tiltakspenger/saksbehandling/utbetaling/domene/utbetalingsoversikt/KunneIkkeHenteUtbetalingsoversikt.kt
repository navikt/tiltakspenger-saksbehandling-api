package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.sikkerloggkontekst

private const val OPERASJON = "henting av utbetalingsoversikt"

/** Feilen bærer sin egen loggkontekst og logges én gang av jobben. */
sealed interface KunneIkkeHenteUtbetalingsoversikt : Loggbar {
    val feiltype: Oppslagsfeiltype
    val metadata: HttpKlientMetadata

    /** Sant når neste sak ville fått samme feil, så en kjøring over flere saker bør stoppe. */
    val rammerAlleSaker: Boolean

    /** Tjenesten avviste oss, ikke personen det ble spurt om. */
    data class TilgangAvvist(
        val httpKlientError: HttpKlientError.UventetStatus,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.TILGANG_AVVIST
        override val rammerAlleSaker = true
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
        override val loggkontekst: Loggkontekst get() = httpKlientError.loggkontekst(OPERASJON)
        override val sikkerloggkontekst: Loggkontekst get() = httpKlientError.sikkerloggkontekst(OPERASJON)
    }

    /** Kallet ga ikke et brukbart svar: ingen respons, request som ikke ble sendt, eller en status vi ikke kjenner. */
    data class Tjenestefeil(
        val httpKlientError: HttpKlientError,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.TJENESTEFEIL
        override val rammerAlleSaker = true
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
        override val loggkontekst: Loggkontekst get() = httpKlientError.loggkontekst(OPERASJON)
        override val sikkerloggkontekst: Loggkontekst get() = httpKlientError.sikkerloggkontekst(OPERASJON)
    }

    /** Tjenesten avviste oppslaget for denne personen. */
    data class SakAvvist(
        val httpKlientError: HttpKlientError.UventetStatus,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.SAK_AVVIST
        override val rammerAlleSaker = false
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
        override val loggkontekst: Loggkontekst get() = httpKlientError.loggkontekst(OPERASJON)
        override val sikkerloggkontekst: Loggkontekst get() = httpKlientError.sikkerloggkontekst(OPERASJON)
    }

    /** Svaret lot seg ikke lese som json i forventet form. */
    data class UleseligSvar(
        val httpKlientError: HttpKlientError.DeserializationError,
    ) : KunneIkkeHenteUtbetalingsoversikt {
        override val feiltype = Oppslagsfeiltype.ULESELIG_SVAR
        override val rammerAlleSaker = false
        override val metadata: HttpKlientMetadata get() = httpKlientError.metadata
        override val loggkontekst: Loggkontekst get() = httpKlientError.loggkontekst(OPERASJON)
        override val sikkerloggkontekst: Loggkontekst get() = httpKlientError.sikkerloggkontekst(OPERASJON)
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
        override val rammerAlleSaker = false
        override val loggkontekst: Loggkontekst get() = Loggkontekst("Feil ved $OPERASJON. Svaret brøt kontrakten: $feil mot ${metadata.endepunkt}")
        override val sikkerloggkontekst: Loggkontekst get() =
            Loggkontekst("Feil ved $OPERASJON. Request: ${metadata.rawRequestString}. Response: ${metadata.rawResponseString}")
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
