package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

/**
 * Hvor langt en sak har kommet i migreringen fra [Oppdragsversjon.V2] til [Oppdragsversjon.V3], se `MigrerTilOppdragsversjon3Service`.
 * En sak som aldri er migrert, har ingen tilstand.
 * Databasen avviser kombinasjoner av tilstand og [Oppdragsversjon] som ikke står under.
 */
enum class Oppdragsmigrering {
    /**
     * Utbetalingene er sperret, og Hel Ved har ikke svart OK på migreringen.
     * Saken står på [Oppdragsversjon.V2].
     */
    SPERRET,

    /**
     * Hel Ved har svart OK, og saken står på [Oppdragsversjon.V3], men Hel Ved har ikke bekreftet at de har tatt opp kjedene.
     * Utbetalingene er fortsatt sperret.
     */
    MIGRERT_HOS_HEL_VED,

    /** Hel Ved har tatt opp kjedene, og utbetalingene sendes på [Oppdragsversjon.V3]. */
    FERDIG,
    ;

    val sperrerUtbetalinger: Boolean get() = this != FERDIG
}
