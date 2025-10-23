package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus

private enum class MeldekortBehandletAutomatiskStatusDb {
    VENTER_BEHANDLING,
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
    KAN_IKKE_MELDE_HELG,
}

fun MeldekortBehandletAutomatiskStatus.tilDb(): String = when (this) {
    MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING -> MeldekortBehandletAutomatiskStatusDb.VENTER_BEHANDLING
    MeldekortBehandletAutomatiskStatus.BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.BEHANDLET
    MeldekortBehandletAutomatiskStatus.UKJENT_FEIL -> MeldekortBehandletAutomatiskStatusDb.UKJENT_FEIL
    MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET -> MeldekortBehandletAutomatiskStatusDb.HENTE_NAVKONTOR_FEILET
    MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.BEHANDLING_FEILET_PÅ_SAK
    MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.UTBETALING_FEILET_PÅ_SAK
    MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK -> MeldekortBehandletAutomatiskStatusDb.SKAL_IKKE_BEHANDLES_AUTOMATISK
    MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.ALLEREDE_BEHANDLET
    MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE
    MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING -> MeldekortBehandletAutomatiskStatusDb.ER_UNDER_REVURDERING
    MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT -> MeldekortBehandletAutomatiskStatusDb.FOR_MANGE_DAGER_REGISTRERT
    MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG -> MeldekortBehandletAutomatiskStatusDb.KAN_IKKE_MELDE_HELG
}.toString()

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
        MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE
        MeldekortBehandletAutomatiskStatusDb.ER_UNDER_REVURDERING -> MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING
        MeldekortBehandletAutomatiskStatusDb.FOR_MANGE_DAGER_REGISTRERT -> MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT
        MeldekortBehandletAutomatiskStatusDb.KAN_IKKE_MELDE_HELG -> MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG
    }
