package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreVedtaksbrev

import java.time.LocalDateTime

/**
 * @param tidspunkt når beslutter sist endret valget.
 * @param saksbehandler valget saksbehandler sendte til beslutning.
 * @param beslutter valget beslutter endret til.
 */
data class MeldekortbehandlingSkalJournalføreVedtaksbrevEndring(
    val tidspunkt: LocalDateTime,
    val saksbehandler: Boolean,
    val beslutter: Boolean,
) {
    init {
        require(saksbehandler != beslutter) { "Beslutters valg må være ulikt saksbehandlers valg." }
    }
}
