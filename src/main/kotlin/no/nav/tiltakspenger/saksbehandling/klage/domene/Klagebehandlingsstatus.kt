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
     */
    OVERSENDT,

    /**
     * Vi har mottatt et svar fra klageinstansen.
     * Basert på hendelsestypen, har vi 3 mulige scenarioer:
     * 1. Klagebehandlingen er allerede ferdigstilt uten at førsteinstansen skal utføre flere handlinger. Saksbehandler bekrefter at de har mottatt svaret og ferdigstiller behandlingen.
     * 2. Vedtaket fra klageinstansen krever videre behandling i førsteinstansen, en omgjøring.
     * 3. RETUR. Innstillingsbrevet er ikke godt nok utredet. Saksbehandler må sende mer informasjon til klageinstansen, setter behandlingen tilbake til [OVERSENDT] og vi avventer en ny hendelse.
     */
    MOTTATT_FRA_KLAGEINSTANS,

    /**
     * Vi har mottatt et svar fra klageinstansen, og resultatet av skal ikke føre til noen videre behandling.
     * Saksbeahndler bare bekrefter at de har mottatt svaret, og at klagebehandlingen er ferdigstilt.
     */
    FERDIGSTILT,
}
