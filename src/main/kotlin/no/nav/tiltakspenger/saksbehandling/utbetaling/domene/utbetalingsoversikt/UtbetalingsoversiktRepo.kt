package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.SakId
import java.time.LocalDateTime

interface UtbetalingsoversiktRepo {
    /** [metadata] har ett element per kall, altså per ident som ble slått opp. */
    fun lagre(oversikt: Utbetalingsoversikt, metadata: NonEmptyList<UtbetalingsoversiktMetadata>)

    /** Henter status for saken, med forrige vellykkede oppslag når det siste feilet. */
    fun hentStatusForSak(sakId: SakId): Utbetalingsoversiktstatus

    /**
     * Henter sakene som skal slås opp ved [nå], med tidligste frist først.
     * Køen tar ikke hensyn til åpningstiden; det gjør kalleren.
     */
    fun hentSakerKlareForOppslag(nå: LocalDateTime, limit: Int): List<SakId>
}
