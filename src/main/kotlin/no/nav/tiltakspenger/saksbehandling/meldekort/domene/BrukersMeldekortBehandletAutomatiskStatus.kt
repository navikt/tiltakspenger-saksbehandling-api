package no.nav.tiltakspenger.saksbehandling.meldekort.domene

enum class BrukersMeldekortBehandletAutomatiskStatus {
    BEHANDLET,
    UKJENT_FEIL,
    HENTE_NAVKONTOR_FEILET,
    BEHANDLING_FEILET_PÅ_SAK,
    UTBETALING_FEILET_PÅ_SAK,
    SKAL_IKKE_BEHANDLES_AUTOMATISK,
    ALLEREDE_BEHANDLET,
    UTDATERT_MELDEPERIODE,
    ER_UNDER_REVURDERING,
    FOR_MANGE_DAGER_REGISTRERT,
}
