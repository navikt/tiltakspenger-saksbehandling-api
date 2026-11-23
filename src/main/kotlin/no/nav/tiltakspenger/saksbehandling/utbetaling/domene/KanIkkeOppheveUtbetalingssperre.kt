package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst

/** Grunner til at utbetalingssperren på en sak ikke kan oppheves, og sperren står til neste forsøk. */
sealed interface KanIkkeOppheveUtbetalingssperre {
    /** En ny simulering av siste sendte utbetaling viser endring, så Hel Ved har kanskje ikke tatt opp de migrerte kjedene. */
    data class SimuleringenViserEndring(val utbetalingId: UtbetalingId) :
        KanIkkeOppheveUtbetalingssperre,
        Loggbar {
        override val loggkontekst = Loggkontekst(
            "Simuleringen av siste sendte utbetaling $utbetalingId viser endring etter migreringen, så Hel Ved har kanskje ikke tatt opp kjedene. Utbetalingssperren står",
        )
    }

    /** Den nye simuleringen av siste sendte utbetaling feilet, så vi vet ikke om Hel Ved har tatt opp de migrerte kjedene. */
    data class SimuleringFeilet(val feil: KunneIkkeSimulere) : KanIkkeOppheveUtbetalingssperre
}
