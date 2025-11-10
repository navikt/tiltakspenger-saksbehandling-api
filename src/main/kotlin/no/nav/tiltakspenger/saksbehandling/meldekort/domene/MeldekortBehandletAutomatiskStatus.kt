package no.nav.tiltakspenger.saksbehandling.meldekort.domene

enum class MeldekortBehandletAutomatiskStatus(val loggesSomError: Boolean) {
    VENTER_BEHANDLING(false),
    BEHANDLET(false),
    SKAL_IKKE_BEHANDLES_AUTOMATISK(false),
    ALLEREDE_BEHANDLET(false),
    ER_UNDER_REVURDERING(false),
    FOR_MANGE_DAGER_GODKJENT_FRAVÆR(false),
    HAR_ÅPEN_BEHANDLING(false),
    MÅ_BEHANDLE_FØRSTE_KJEDE(false),
    MÅ_BEHANDLE_NESTE_KJEDE(false),

    INGEN_DAGER_GIR_RETT(true),
    UKJENT_FEIL(true),
    HENTE_NAVKONTOR_FEILET(true),
    BEHANDLING_FEILET_PÅ_SAK(true),
    UTBETALING_FEILET_PÅ_SAK(true),
    UTDATERT_MELDEPERIODE(true),
    FOR_MANGE_DAGER_REGISTRERT(true),
    KAN_IKKE_MELDE_HELG(true),
}
