package no.nav.tiltakspenger.saksbehandling.domene.vedtak

/**
 * Når vi journalfører utbetalingsvedtakene tar vi med tiltaksdata som er relevant for journalføring, men som vi ikke persisterer på utbetalingsvedtaket.
 */
data class TiltaksdataForJournalføring(
    val tiltakstype: String,
    val tiltaksnavn: String,
    val eksternGjennomføringId: String?,
    val eksternDeltagelseId: String,
)
