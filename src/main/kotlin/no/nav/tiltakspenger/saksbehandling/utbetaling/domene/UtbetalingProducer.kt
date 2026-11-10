package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

interface UtbetalingProducer {
    /**
     * Produserer utbetalingen til Hel Ved og venter på at Kafka har bekreftet den.
     * Nøkkelen er [utbetalingId], og Hel Ved bruker den samme nøkkelen på statusmeldingene for utbetalingen.
     */
    fun produser(utbetalingId: UtbetalingId, helvedUtbetalingJson: String)
}
