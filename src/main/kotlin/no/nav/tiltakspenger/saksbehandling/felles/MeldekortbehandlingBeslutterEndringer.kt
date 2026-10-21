package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat.MeldekortbehandlingSkalJournalføreNotatEndring
import java.time.LocalDateTime

data class MeldekortbehandlingBeslutterEndringer(
    val skalJournalføreNotat: MeldekortbehandlingSkalJournalføreNotatEndring?,
) {
    fun oppdater(
        gjeldendeValg: Boolean,
        besluttersValg: Boolean,
        tidspunkt: LocalDateTime,
    ): MeldekortbehandlingBeslutterEndringer {
        val saksbehandlersValg = skalJournalføreNotat?.saksbehandler ?: gjeldendeValg
        return copy(
            skalJournalføreNotat = if (besluttersValg == saksbehandlersValg) {
                null
            } else {
                MeldekortbehandlingSkalJournalføreNotatEndring(tidspunkt, saksbehandlersValg, besluttersValg)
            },
        )
    }
}
