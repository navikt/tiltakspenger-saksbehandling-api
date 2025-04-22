package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus

private enum class MeldekortBehandletAutomatiskStatusDb {
    BEHANDLET,
    UKJENT_FEIL,
    HENTE_NAVKONTOR_FEILET,
    BEHANDLING_FEILET_PÅ_SAK,
    UTBETALING_FEILET_PÅ_SAK,
    SKAL_IKKE_BEHANDLES_AUTOMATISK,
    TIDLIGERE_BEHANDLET,
    UTDATERT_MELDEPERIODE,
}

fun MeldekortBehandletAutomatiskStatus.tilDb(): String = when (this) {
    MeldekortBehandletAutomatiskStatus.BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.BEHANDLET
    MeldekortBehandletAutomatiskStatus.UKJENT_FEIL -> MeldekortBehandletAutomatiskStatusDb.UKJENT_FEIL
    MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET -> MeldekortBehandletAutomatiskStatusDb.HENTE_NAVKONTOR_FEILET
    MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.BEHANDLING_FEILET_PÅ_SAK
    MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.UTBETALING_FEILET_PÅ_SAK
    MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK -> MeldekortBehandletAutomatiskStatusDb.SKAL_IKKE_BEHANDLES_AUTOMATISK
    MeldekortBehandletAutomatiskStatus.TIDLIGERE_BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.TIDLIGERE_BEHANDLET
    MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE
}.toString()

fun String.tilMeldekortBehandletAutomatiskStatus(): MeldekortBehandletAutomatiskStatus =
    deserialize<MeldekortBehandletAutomatiskStatusDb>(this).let {
        when (it) {
            MeldekortBehandletAutomatiskStatusDb.BEHANDLET -> MeldekortBehandletAutomatiskStatus.BEHANDLET
            MeldekortBehandletAutomatiskStatusDb.UKJENT_FEIL -> MeldekortBehandletAutomatiskStatus.UKJENT_FEIL
            MeldekortBehandletAutomatiskStatusDb.HENTE_NAVKONTOR_FEILET -> MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET
            MeldekortBehandletAutomatiskStatusDb.BEHANDLING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK
            MeldekortBehandletAutomatiskStatusDb.UTBETALING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK
            MeldekortBehandletAutomatiskStatusDb.SKAL_IKKE_BEHANDLES_AUTOMATISK -> MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK
            MeldekortBehandletAutomatiskStatusDb.TIDLIGERE_BEHANDLET -> MeldekortBehandletAutomatiskStatus.TIDLIGERE_BEHANDLET
            MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE
        }
    }
