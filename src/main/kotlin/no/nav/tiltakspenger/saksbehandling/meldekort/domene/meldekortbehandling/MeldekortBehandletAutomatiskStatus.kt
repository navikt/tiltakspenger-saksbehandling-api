package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

enum class MeldekortBehandletAutomatiskStatus(val loggesSomError: Boolean, val kanPrøvesPåNytt: Boolean) {
    VENTER_BEHANDLING(loggesSomError = false, kanPrøvesPåNytt = false),
    BEHANDLET(loggesSomError = false, kanPrøvesPåNytt = false),
    SKAL_IKKE_BEHANDLES_AUTOMATISK(loggesSomError = false, kanPrøvesPåNytt = false),
    ALLEREDE_BEHANDLET(loggesSomError = false, kanPrøvesPåNytt = false),
    ER_UNDER_REVURDERING(loggesSomError = false, kanPrøvesPåNytt = true),
    FOR_MANGE_DAGER_GODKJENT_FRAVÆR(loggesSomError = false, kanPrøvesPåNytt = false),
    HAR_ÅPEN_BEHANDLING(loggesSomError = false, kanPrøvesPåNytt = true),
    MÅ_BEHANDLE_FØRSTE_KJEDE(loggesSomError = false, kanPrøvesPåNytt = true),
    MÅ_BEHANDLE_NESTE_KJEDE(loggesSomError = false, kanPrøvesPåNytt = true),
    INGEN_DAGER_GIR_RETT(loggesSomError = false, kanPrøvesPåNytt = false),

    UKJENT_FEIL(loggesSomError = true, kanPrøvesPåNytt = true),
    HENTE_NAVKONTOR_FEILET(loggesSomError = true, kanPrøvesPåNytt = true),
    BEHANDLING_FEILET_PÅ_SAK(loggesSomError = true, kanPrøvesPåNytt = false),
    UTBETALING_FEILET_PÅ_SAK(loggesSomError = true, kanPrøvesPåNytt = false),
    UTDATERT_MELDEPERIODE(loggesSomError = true, kanPrøvesPåNytt = false),
    FOR_MANGE_DAGER_REGISTRERT(loggesSomError = true, kanPrøvesPåNytt = false),
    KAN_IKKE_MELDE_HELG(loggesSomError = true, kanPrøvesPåNytt = false),
    HAR_FEILUTBETALING(loggesSomError = true, kanPrøvesPåNytt = false),
    HAR_JUSTERING(loggesSomError = true, kanPrøvesPåNytt = false),
}
