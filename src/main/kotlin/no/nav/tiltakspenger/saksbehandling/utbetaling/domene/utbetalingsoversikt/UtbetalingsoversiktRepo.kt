package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.SakId
import java.time.LocalDateTime

interface UtbetalingsoversiktRepo {
    fun lagre(oversikt: Utbetalingsoversikt, metadata: UtbetalingsoversiktMetadata)

    /** Henter status for saken, med forrige vellykkede oppslag når det siste feilet. */
    fun hentStatusForSak(sakId: SakId): Utbetalingsoversiktstatus

    /**
     * Henter sakene som skal slås opp ved [nå], med tidligste frist først.
     * Køen tar ikke hensyn til åpningstiden; det gjør kalleren.
     */
    fun hentSakerKlareForOppslag(nå: LocalDateTime, limit: Int): List<SakId>
}
