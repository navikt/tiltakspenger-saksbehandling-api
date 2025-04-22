package no.nav.tiltakspenger.saksbehandling.meldekort.domene

enum class MeldekortBehandletAutomatiskStatus {
    BEHANDLET,
    UKJENT_FEIL,
    HENTE_NAVKONTOR_FEILET,
    BEHANDLING_FEILET_PÅ_SAK,
    UTBETALING_FEILET_PÅ_SAK,
    SKAL_IKKE_BEHANDLES_AUTOMATISK,
    TIDLIGERE_BEHANDLET,
    UTDATERT_MELDEPERIODE,
}
