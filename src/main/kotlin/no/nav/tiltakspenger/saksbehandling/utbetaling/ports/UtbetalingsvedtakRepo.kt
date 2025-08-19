package no.nav.tiltakspenger.saksbehandling.utbetaling.ports

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import java.time.LocalDateTime

interface UtbetalingsvedtakRepo {
    fun lagre(vedtak: MeldekortVedtak, context: TransactionContext? = null)

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

    fun hentUtbetalingsvedtakForUtsjekk(limit: Int = 10): List<MeldekortVedtak>

    fun hentDeSomSkalJournalføres(limit: Int = 10): List<MeldekortVedtak>

    fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext? = null,
    )

    fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int = 10): List<UtbetalingDetSkalHentesStatusFor>
}
