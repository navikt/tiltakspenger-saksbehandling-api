package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.distribuereVedtaksbrev.MeldekortbehandlingSkalDistribuereVedtaksbrevEndring
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat.MeldekortbehandlingSkalJournalføreNotatEndring
import java.time.LocalDateTime

data class MeldekortbehandlingBeslutterEndringer(
    val skalJournalføreNotat: MeldekortbehandlingSkalJournalføreNotatEndring?,
    val skalDistribuereVedtaksbrev: MeldekortbehandlingSkalDistribuereVedtaksbrevEndring?,
) {
    fun oppdaterSkalJournalføreNotat(
        gjeldendeValg: Boolean,
        besluttersValg: Boolean,
        tidspunkt: LocalDateTime,
    ): MeldekortbehandlingBeslutterEndringer {
        if (besluttersValg == gjeldendeValg) return this
        val saksbehandlersValg = skalJournalføreNotat?.saksbehandler ?: gjeldendeValg
        return copy(
            skalJournalføreNotat = if (besluttersValg == saksbehandlersValg) {
                null
            } else {
                MeldekortbehandlingSkalJournalføreNotatEndring(tidspunkt, saksbehandlersValg, besluttersValg)
            },
        )
    }

    fun oppdaterSkalDistribuereVedtaksbrev(
        gjeldendeValg: Boolean,
        besluttersValg: Boolean,
        tidspunkt: LocalDateTime,
    ): MeldekortbehandlingBeslutterEndringer {
        if (besluttersValg == gjeldendeValg) return this
        val saksbehandlersValg = skalDistribuereVedtaksbrev?.saksbehandler ?: gjeldendeValg
        return copy(
            skalDistribuereVedtaksbrev = if (besluttersValg == saksbehandlersValg) {
                null
            } else {
                MeldekortbehandlingSkalDistribuereVedtaksbrevEndring(tidspunkt, saksbehandlersValg, besluttersValg)
            },
        )
    }
}
