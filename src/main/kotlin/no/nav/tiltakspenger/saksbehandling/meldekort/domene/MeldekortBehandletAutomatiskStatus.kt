package no.nav.tiltakspenger.saksbehandling.meldekort.domene

enum class MeldekortBehandletAutomatiskStatus(val loggesSomError: Boolean) {
    VENTER_BEHANDLING(false),
    BEHANDLET(false),
    UKJENT_FEIL(true),
    HENTE_NAVKONTOR_FEILET(true),
    BEHANDLING_FEILET_PÅ_SAK(true),
    UTBETALING_FEILET_PÅ_SAK(true),
    SKAL_IKKE_BEHANDLES_AUTOMATISK(false),
    ALLEREDE_BEHANDLET(false),
    UTDATERT_MELDEPERIODE(true),
    ER_UNDER_REVURDERING(false),
    FOR_MANGE_DAGER_REGISTRERT(true),
    KAN_IKKE_MELDE_HELG(true),
    FOR_MANGE_DAGER_GODKJENT_FRAVÆR(false),
}
