package no.nav.tiltakspenger.saksbehandling.klage.domene

enum class Klagebehandlingsstatus {
    /** Det står ikke en saksbehandler på behandlingen */
    KLAR_TIL_BEHANDLING,

    /** En saksbehandler står på behandlingen. */
    UNDER_BEHANDLING,

    /** Klagebehandlingen er avsluttet. Dette er en en endelig tilstand.*/
    AVBRUTT,

    /** Andre ord som iverksatt og ferdigstilt brukes også. Kan brukes både ved avvisning og medhold, men også etter klageinstansens avgjørelse */
    VEDTATT,

    /**
     * Saksbehandler har opprettholdt og bedt at den oversendes til klageinstansen.
     * En jobb plukker opp disse statusene, journalfører og distribuerer innstillingsbrev og oversender klagen til klageinstansen.
     * Fra saksbehenandler sitt perspektiv, merk at vi anser behandlingen som "avsluttet", frem til klageinstansen har iverksatt ett vedtak.
     * Så denne statusen skal ikke vises i benken.
     */
    OPPRETTHOLDT,

    /**
     * Vi har journalført+distribuert innstillingsbrevet og oversendt klagen til klageinstansen.
     * Fra saksbehenandler sitt perspektiv, merk at vi anser behandlingen som "avsluttet", frem til klageinstansen har iverksatt ett vedtak.
     * Så denne statusen skal ikke vises i benken.
     *
     * */
    OVERSENDT,

    /**
     * Vi har mottatt et svar fra klageinstansen, og resultatet av skal ikke føre til noen videre behandling.
     * Saksbeahndler bare bekrefter at de har mottatt svaret, og at klagebehandlingen er ferdigstilt.
     */
    FERDIGSTILT,
}
