package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat

import java.time.LocalDateTime

/**
 * @param tidspunkt når beslutter sist endret valget.
 * @param saksbehandler valget saksbehandler sendte til beslutning.
 * @param beslutter valget beslutter endret til, og som er det gjeldende valget.
 */
data class MeldekortbehandlingSkalJournalføreNotatEndring(
    val tidspunkt: LocalDateTime,
    val saksbehandler: Boolean,
    val beslutter: Boolean,
) {
    init {
        require(saksbehandler != beslutter) { "Beslutters valg må være ulikt saksbehandlers valg." }
    }
}
