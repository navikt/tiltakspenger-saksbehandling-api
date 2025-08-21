package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import java.time.LocalDateTime
import kotlin.collections.set

class UtbetalingFakeRepo : UtbetalingRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<UtbetalingId, Utbetaling>())

    override fun lagre(
        utbetaling: Utbetaling,
        context: TransactionContext?,
    ) {
        data.get()[utbetaling.id] = utbetaling
    }

    override fun markerSendtTilUtbetaling(
        vedtakId: VedtakId,
        tidspunkt: LocalDateTime,
        utbetalingsrespons: SendtUtbetaling,
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreFeilResponsFraUtbetaling(
        vedtakId: VedtakId,
        utbetalingsrespons: KunneIkkeUtbetale,
    ) {
        TODO("Not yet implemented")
    }

    override fun hentUtbetalingJsonForVedtakId(vedtakId: VedtakId): String? {
        TODO("Not yet implemented")
    }

    override fun hentForUtsjekk(limit: Int): List<MeldekortVedtak> {
        TODO("Not yet implemented")
    }

    override fun oppdaterUtbetalingsstatus(
        vedtakId: VedtakId,
        status: Utbetalingsstatus,
        metadata: Forsøkshistorikk,
        context: TransactionContext?,
    ) {
        TODO("Not yet implemented")
    }

    override fun hentDeSomSkalHentesUtbetalingsstatusFor(limit: Int): List<UtbetalingDetSkalHentesStatusFor> {
        TODO("Not yet implemented")
    }
}
