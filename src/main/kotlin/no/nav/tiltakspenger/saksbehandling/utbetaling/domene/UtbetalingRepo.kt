package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import java.time.LocalDateTime

interface UtbetalingRepo {
    /**
     * Krever en transaksjon fordi markeringen skal rulles tilbake dersom iverksettingen mot helved feiler.
     */
    fun markerSendtTilUtbetaling(
        utbetalingId: UtbetalingId,
        tidspunkt: LocalDateTime,
        context: TransactionContext,
    )

    /**
     * Lagrer requesten vi sendte til helved og svaret vi fikk, enten kallet lyktes eller feilet.
     */
    fun lagreUtbetalingsrespons(
        utbetalingId: UtbetalingId,
        utbetalingsrespons: UtbetalingResponse,
        context: TransactionContext? = null,
    )

    fun hentUtbetalingJson(utbetalingId: UtbetalingId): String?

    fun hentForUtsjekk(limit: Int = 10): List<VedtattUtbetaling>

    fun oppdaterUtbetalingsstatus(
        utbetalingId: UtbetalingId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext? = null,
    )

    fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int = 10): List<UtbetalingDetSkalHentesStatusFor>
}
