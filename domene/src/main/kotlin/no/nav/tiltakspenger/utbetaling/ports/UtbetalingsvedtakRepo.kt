package no.nav.tiltakspenger.utbetaling.ports

import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak
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
}
