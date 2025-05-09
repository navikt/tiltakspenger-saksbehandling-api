package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

/**
 * Nåværende versjon: https://helved-docs.intern.dev.nav.no/v2/doc/status
 *
 * Neste versjon: https://helved-docs.intern.dev.nav.no/v3/doc/sjekk_status_pa_en_utbetaling
 */
enum class Utbetalingsstatus {
    /** Ikke sendt til økonomisystemet */
    IkkePåbegynt,

    /** Sendt til økonomisystemet, venter på svar */
    SendtTilOppdrag,

    /** Feilkvittering fra økonomisystemet. Kan være enten teknisk eller funksjonell feil. */
    FeiletMotOppdrag,

    /** Kvittert OK fra økonomisystemet og ferdigstilt. */
    Ok,

    /** Ferdigstilt uten å ha blitt sendt til økonomisystemet pga. at det ikke er noe å utbetale */
    OkUtenUtbetaling,

    Avbrutt,

    ;

    fun erOK(): Boolean {
        return this == Ok || this == OkUtenUtbetaling
    }
}
