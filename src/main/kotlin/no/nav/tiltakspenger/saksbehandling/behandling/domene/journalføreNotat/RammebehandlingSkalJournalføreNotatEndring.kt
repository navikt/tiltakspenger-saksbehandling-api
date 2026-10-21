package no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat

import java.time.LocalDateTime

/**
 * Bare begrunnelsene beslutter har endret valget for, er med.
 *
 * @param tidspunkt når beslutter sist endret valget.
 */
data class RammebehandlingSkalJournalføreNotatEndring(
    val tidspunkt: LocalDateTime,
    val vilkårsvurdering: VilkårsvurderingSkalJournalføreNotatEndring?,
    val barnetillegg: BarnetilleggSkalJournalføreNotatEndring?,
) {
    init {
        require(vilkårsvurdering != null || barnetillegg != null) {
            "Minst ett av valgene må være endret av beslutter."
        }
    }
}

/**
 * @param saksbehandler det opprinnelige valget
 * @param beslutter beslutter overstyrte valg
 */
data class VilkårsvurderingSkalJournalføreNotatEndring(
    val saksbehandler: Boolean,
    val beslutter: Boolean,
) {
    init {
        require(saksbehandler != beslutter) { "Beslutters valg må være ulikt saksbehandlers valg." }
    }
}

/**
 * @param saksbehandler det opprinnelige valget
 * @param beslutter beslutter overstyrte valg
 */
data class BarnetilleggSkalJournalføreNotatEndring(
    val saksbehandler: Boolean,
    val beslutter: Boolean,
) {
    init {
        require(saksbehandler != beslutter) { "Beslutters valg må være ulikt saksbehandlers valg." }
    }
}
