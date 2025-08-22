package no.nav.tiltakspenger.saksbehandling.utbetaling.ports

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import java.time.LocalDateTime

interface UtbetalingRepo {
    fun lagre(utbetaling: Utbetaling, context: TransactionContext? = null)

    fun markerSendtTilUtbetaling(
        utbetalingId: UtbetalingId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    )

    fun lagreFeilResponsFraUtbetaling(
        vedtakId: VedtakId,
        utbetalingsrespons: KunneIkkeUtbetale,
    )

    fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String?

    fun hentForUtsjekk(limit: Int = 10): List<Utbetaling>

    fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext? = null,
    )

    fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int = 10): List<UtbetalingDetSkalHentesStatusFor>
}
