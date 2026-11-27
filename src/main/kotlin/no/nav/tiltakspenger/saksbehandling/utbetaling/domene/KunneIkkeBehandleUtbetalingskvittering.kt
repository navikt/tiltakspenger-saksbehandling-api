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

    data class UgyldigInnhold(val årsak: Årsak) : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = false

        enum class Årsak(val loggmelding: String) {
            MeldingErIkkeObjekt("Meldingen må være et JSON-objekt"),
            SimuleringErIkkeBoolsk("simulering må være boolsk når feltet er satt"),
            DetaljerErIkkeObjekt("detaljer må være et objekt eller null"),
            LinjerErIkkeListe("detaljer.linjer må være en liste"),
            LinjeErIkkeObjekt("Hvert element i detaljer.linjer må være et objekt"),
            UgyldigBehandlingId("detaljer.linjer.behandlingId må være en streng på 15 tegn"),
            StatusErIkkeStreng("status må være en streng"),
            UkjentStatus("status har en ukjent verdi"),
        }
    }

    data class FantIkkeUtbetaling(val utbetalingId: UtbetalingId) : KunneIkkeBehandleUtbetalingskvittering {
        // Kvitteringen kan komme før transaksjonen som sendte utbetalingen er synlig.
        override val forbigående = true
    }

    data class ManglerUtbetalingskobling(val årsak: Årsak) : KunneIkkeBehandleUtbetalingskvittering {
        override val forbigående = true

        enum class Årsak(val loggmelding: String) {
            ManglerReferanser("Kvitteringen mangler utbetalingsreferanser"),
            IngenTreffPåReferanser("Ingen utbetaling matcher referansene i kvitteringen"),
            ManglerLagretUtbetalingId("Kvitteringen mangler lagret utbetalings-ID"),
            IngenKobletUtbetaling("Ingen utbetaling er koblet til statuskvitteringen"),
        }
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
