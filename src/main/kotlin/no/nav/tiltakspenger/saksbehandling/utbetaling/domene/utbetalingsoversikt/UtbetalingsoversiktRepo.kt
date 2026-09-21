package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.SakId

interface UtbetalingsoversiktRepo {
    fun lagre(oversikt: Utbetalingsoversikt, metadata: UtbetalingsoversiktMetadata)

    /** Henter status for saken, med forrige vellykkede oppslag når det siste feilet. */
    fun hentStatusForSak(sakId: SakId): Utbetalingsoversiktstatus
}
