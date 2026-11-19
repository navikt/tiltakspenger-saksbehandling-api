package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus

private enum class MeldekortBehandletAutomatiskStatusDb {
    VENTER_BEHANDLING,
    BEHANDLET,
    UKJENT_FEIL,
    HENTE_NAVKONTOR_FEILET,
    BEHANDLING_FEILET_PÅ_SAK,
    UTBETALING_FEILET_PÅ_SAK,
    SKAL_IKKE_BEHANDLES_AUTOMATISK,
    ALLEREDE_BEHANDLET,
    FOR_SENT_Å_KORRIGERE,
    TIDLIGERE_BEHANDLET_MANUELT,
    NYERE_INNSENDING_ER_BEHANDLET,
    UTDATERT_MELDEPERIODE,
    ER_UNDER_REVURDERING,
    FOR_MANGE_DAGER_REGISTRERT,
    KAN_IKKE_MELDE_HELG,
    FOR_MANGE_DAGER_GODKJENT_FRAVÆR,
    HAR_ÅPEN_BEHANDLING,
    MÅ_BEHANDLE_FØRSTE_KJEDE,
    MÅ_BEHANDLE_NESTE_KJEDE,
    INGEN_DAGER_GIR_RETT,
    HAR_JUSTERING,
    HAR_TIDLIGERE_UTBETALING,
    HAR_FEILUTBETALING,
    SIMULERING_FEILET,
}

// ALLEREDE_BEHANDLET må fortsatt kunne lagres og leses for historikken.
@Suppress("DEPRECATION")
fun MeldekortBehandletAutomatiskStatus.tilDb(): String = when (this) {
    MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING -> MeldekortBehandletAutomatiskStatusDb.VENTER_BEHANDLING
    MeldekortBehandletAutomatiskStatus.BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.BEHANDLET
    MeldekortBehandletAutomatiskStatus.UKJENT_FEIL -> MeldekortBehandletAutomatiskStatusDb.UKJENT_FEIL
    MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET -> MeldekortBehandletAutomatiskStatusDb.HENTE_NAVKONTOR_FEILET
    MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.BEHANDLING_FEILET_PÅ_SAK
    MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.UTBETALING_FEILET_PÅ_SAK
    MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK -> MeldekortBehandletAutomatiskStatusDb.SKAL_IKKE_BEHANDLES_AUTOMATISK
    MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.ALLEREDE_BEHANDLET
    MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE -> MeldekortBehandletAutomatiskStatusDb.FOR_SENT_Å_KORRIGERE
    MeldekortBehandletAutomatiskStatus.TIDLIGERE_BEHANDLET_MANUELT -> MeldekortBehandletAutomatiskStatusDb.TIDLIGERE_BEHANDLET_MANUELT
    MeldekortBehandletAutomatiskStatus.NYERE_INNSENDING_ER_BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.NYERE_INNSENDING_ER_BEHANDLET
    MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE
    MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING -> MeldekortBehandletAutomatiskStatusDb.ER_UNDER_REVURDERING
    MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT -> MeldekortBehandletAutomatiskStatusDb.FOR_MANGE_DAGER_REGISTRERT
    MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG -> MeldekortBehandletAutomatiskStatusDb.KAN_IKKE_MELDE_HELG
    MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR -> MeldekortBehandletAutomatiskStatusDb.FOR_MANGE_DAGER_GODKJENT_FRAVÆR
    MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING -> MeldekortBehandletAutomatiskStatusDb.HAR_ÅPEN_BEHANDLING
    MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE -> MeldekortBehandletAutomatiskStatusDb.MÅ_BEHANDLE_FØRSTE_KJEDE
    MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE -> MeldekortBehandletAutomatiskStatusDb.MÅ_BEHANDLE_NESTE_KJEDE
    MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT -> MeldekortBehandletAutomatiskStatusDb.INGEN_DAGER_GIR_RETT
    MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING -> MeldekortBehandletAutomatiskStatusDb.HAR_FEILUTBETALING
    MeldekortBehandletAutomatiskStatus.HAR_JUSTERING -> MeldekortBehandletAutomatiskStatusDb.HAR_JUSTERING
    MeldekortBehandletAutomatiskStatus.HAR_TIDLIGERE_UTBETALING -> MeldekortBehandletAutomatiskStatusDb.HAR_TIDLIGERE_UTBETALING
    MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET -> MeldekortBehandletAutomatiskStatusDb.SIMULERING_FEILET
}.toString()

@Suppress("DEPRECATION")
fun String.tilMeldekortBehandletAutomatiskStatus(): MeldekortBehandletAutomatiskStatus =
    when (MeldekortBehandletAutomatiskStatusDb.valueOf(this)) {
        MeldekortBehandletAutomatiskStatusDb.VENTER_BEHANDLING -> MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING
        MeldekortBehandletAutomatiskStatusDb.BEHANDLET -> MeldekortBehandletAutomatiskStatus.BEHANDLET
        MeldekortBehandletAutomatiskStatusDb.UKJENT_FEIL -> MeldekortBehandletAutomatiskStatus.UKJENT_FEIL
        MeldekortBehandletAutomatiskStatusDb.HENTE_NAVKONTOR_FEILET -> MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET
        MeldekortBehandletAutomatiskStatusDb.BEHANDLING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK
        MeldekortBehandletAutomatiskStatusDb.UTBETALING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK
        MeldekortBehandletAutomatiskStatusDb.SKAL_IKKE_BEHANDLES_AUTOMATISK -> MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK
        MeldekortBehandletAutomatiskStatusDb.ALLEREDE_BEHANDLET -> MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET
        MeldekortBehandletAutomatiskStatusDb.FOR_SENT_Å_KORRIGERE -> MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE
        MeldekortBehandletAutomatiskStatusDb.TIDLIGERE_BEHANDLET_MANUELT -> MeldekortBehandletAutomatiskStatus.TIDLIGERE_BEHANDLET_MANUELT
        MeldekortBehandletAutomatiskStatusDb.NYERE_INNSENDING_ER_BEHANDLET -> MeldekortBehandletAutomatiskStatus.NYERE_INNSENDING_ER_BEHANDLET
        MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE
        MeldekortBehandletAutomatiskStatusDb.ER_UNDER_REVURDERING -> MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING
        MeldekortBehandletAutomatiskStatusDb.FOR_MANGE_DAGER_REGISTRERT -> MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT
        MeldekortBehandletAutomatiskStatusDb.KAN_IKKE_MELDE_HELG -> MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG
        MeldekortBehandletAutomatiskStatusDb.FOR_MANGE_DAGER_GODKJENT_FRAVÆR -> MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR
        MeldekortBehandletAutomatiskStatusDb.HAR_ÅPEN_BEHANDLING -> MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING
        MeldekortBehandletAutomatiskStatusDb.MÅ_BEHANDLE_FØRSTE_KJEDE -> MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
        MeldekortBehandletAutomatiskStatusDb.MÅ_BEHANDLE_NESTE_KJEDE -> MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE
        MeldekortBehandletAutomatiskStatusDb.INGEN_DAGER_GIR_RETT -> MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT
        MeldekortBehandletAutomatiskStatusDb.HAR_JUSTERING -> MeldekortBehandletAutomatiskStatus.HAR_JUSTERING
        MeldekortBehandletAutomatiskStatusDb.HAR_TIDLIGERE_UTBETALING -> MeldekortBehandletAutomatiskStatus.HAR_TIDLIGERE_UTBETALING
        MeldekortBehandletAutomatiskStatusDb.HAR_FEILUTBETALING -> MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING
        MeldekortBehandletAutomatiskStatusDb.SIMULERING_FEILET -> MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET
    }
