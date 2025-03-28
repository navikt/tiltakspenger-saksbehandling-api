package no.nav.tiltakspenger.saksbehandling.utbetaling.ports

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import java.time.LocalDateTime

interface UtbetalingsvedtakRepo {
    fun lagre(vedtak: Utbetalingsvedtak, context: TransactionContext? = null)

    fun markerSendtTilUtbetaling(
        vedtakId: VedtakId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    )

    fun lagreFeilResponsFraUtbetaling(
        vedtakId: VedtakId,
        utbetalingsrespons: KunneIkkeUtbetale,
    )

    fun markerJournalført(
        vedtakId: VedtakId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    )

    fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String?

    fun hentUtbetalingsvedtakForUtsjekk(limit: Int = 10): List<Utbetalingsvedtak>

    fun hentDeSomSkalJournalføres(limit: Int = 10): List<Utbetalingsvedtak>

    fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        context: TransactionContext? = null,
    )

    fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int = 10): List<UtbetalingDetSkalHentesStatusFor>
}
