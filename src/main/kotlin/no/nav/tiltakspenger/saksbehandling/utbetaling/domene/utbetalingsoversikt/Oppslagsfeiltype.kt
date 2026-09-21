package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

/** Hvorfor et oppslag mot økonomisystemet feilet, én verdi per variant av [KunneIkkeHenteUtbetalingsoversikt]. */
enum class Oppslagsfeiltype {
    TILGANG_AVVIST,
    TJENESTEFEIL,
    SAK_AVVIST,
    ULESELIG_SVAR,
    UGYLDIG_INNHOLD,
}
