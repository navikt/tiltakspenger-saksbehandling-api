package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

/**
 * Hvilken versjon av Hel Veds grensesnitt utbetalingene i en sak går gjennom.
 * Versjonen gjelder hele saken, fordi Hel Ved bare kjeder utbetalinger innenfor én versjon og avviser versjon 2 når saken er migrert.
 */
enum class Oppdragsversjon {
    /** Hel Ved svarer når utbetalingen sendes, og vi spør etter status senere. */
    V2,

    /** Hel Ved tar imot hele tilstanden for saken og sender status når den er klar. */
    V3,
}
