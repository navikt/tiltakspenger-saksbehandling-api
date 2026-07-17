package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import io.github.oshai.kotlinlogging.Level

/**
 * Utfallet av et forsøk på automatisk behandling av brukers meldekort.
 * [loggnivå] er kilden til hvilket nivå utfallet logges på, både for den generiske statuslinjen og eventuelle utdypende logglinjer.
 * [Level.ERROR] betyr at noe er feil hos oss og at en utvikler bør se på det.
 * [Level.WARN] er forventede domeneutfall der meldekortet i stedet går til manuell behandling hos saksbehandler.
 * [Level.INFO] er normal flyt.
 */
enum class MeldekortBehandletAutomatiskStatus(val loggnivå: Level, val kanPrøvesPåNytt: Boolean) {
    VENTER_BEHANDLING(loggnivå = Level.INFO, kanPrøvesPåNytt = false),
    BEHANDLET(loggnivå = Level.INFO, kanPrøvesPåNytt = false),
    SKAL_IKKE_BEHANDLES_AUTOMATISK(loggnivå = Level.INFO, kanPrøvesPåNytt = false),
    ALLEREDE_BEHANDLET(loggnivå = Level.INFO, kanPrøvesPåNytt = false),
    ER_UNDER_REVURDERING(loggnivå = Level.INFO, kanPrøvesPåNytt = true),
    FOR_MANGE_DAGER_GODKJENT_FRAVÆR(loggnivå = Level.INFO, kanPrøvesPåNytt = false),
    HAR_ÅPEN_BEHANDLING(loggnivå = Level.INFO, kanPrøvesPåNytt = true),
    MÅ_BEHANDLE_FØRSTE_KJEDE(loggnivå = Level.INFO, kanPrøvesPåNytt = true),
    MÅ_BEHANDLE_NESTE_KJEDE(loggnivå = Level.INFO, kanPrøvesPåNytt = true),
    INGEN_DAGER_GIR_RETT(loggnivå = Level.INFO, kanPrøvesPåNytt = false),

    UKJENT_FEIL(loggnivå = Level.ERROR, kanPrøvesPåNytt = true),
    HENTE_NAVKONTOR_FEILET(loggnivå = Level.ERROR, kanPrøvesPåNytt = true),
    BEHANDLING_FEILET_PÅ_SAK(loggnivå = Level.ERROR, kanPrøvesPåNytt = false),
    UTBETALING_FEILET_PÅ_SAK(loggnivå = Level.ERROR, kanPrøvesPåNytt = false),
    UTDATERT_MELDEPERIODE(loggnivå = Level.ERROR, kanPrøvesPåNytt = false),
    FOR_MANGE_DAGER_REGISTRERT(loggnivå = Level.ERROR, kanPrøvesPåNytt = false),
    KAN_IKKE_MELDE_HELG(loggnivå = Level.ERROR, kanPrøvesPåNytt = false),

    /** Simuleringen viser feilutbetaling eller justering, som automatisk behandling ikke støtter. */
    HAR_FEILUTBETALING(loggnivå = Level.WARN, kanPrøvesPåNytt = false),
    HAR_JUSTERING(loggnivå = Level.WARN, kanPrøvesPåNytt = false),
}
