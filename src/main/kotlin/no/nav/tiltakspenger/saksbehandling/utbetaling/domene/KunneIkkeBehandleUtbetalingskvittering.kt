package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

/**
 * Hvorfor en utbetalingskvittering ikke kunne behandles.
 * Jobben prøver behandlingen på nytt med backoff, også etter feil som krever at kode eller data rettes.
 * Dette sender ikke utbetalingen på nytt til utbetalingstjenesten.
 */
sealed interface KunneIkkeBehandleUtbetalingskvittering {
    /** true når feilen kan gå over av seg selv, false når koden eller dataene må rettes før et nytt forsøk lykkes. */
    val forbigående: Boolean

    data object UgyldigNøkkel : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = false
    }

    data object ManglerInnhold : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = false
    }

    data class KunneIkkeTolkeInnhold(val feil: Throwable) : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = false
    }

    data object UgyldigInnhold : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = false
    }

    data class FantIkkeUtbetaling(val utbetalingId: UtbetalingId) : KunneIkkeBehandleUtbetalingskvittering {
        // Kvitteringen kan komme før transaksjonen som sendte utbetalingen er synlig.
        override val forbigående = true
    }

    data object ManglerUtbetalingskobling : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = true
    }

    data object TvetydigUtbetalingskobling : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = true
    }

    data object LagretUtbetalingskoblingAvviker : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = false
    }

    data class UventetFeil(val feil: Throwable) : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = true
    }
}
