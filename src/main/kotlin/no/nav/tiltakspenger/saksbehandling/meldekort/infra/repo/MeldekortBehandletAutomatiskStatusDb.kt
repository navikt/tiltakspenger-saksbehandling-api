package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus

private enum class MeldekortBehandletAutomatiskStatusDb {
    BEHANDLET,
    UKJENT_FEIL,
    HENTE_NAVKONTOR_FEILET,
    BEHANDLING_FEILET_PÅ_SAK,
    UTBETALING_FEILET_PÅ_SAK,
    SKAL_IKKE_BEHANDLES_AUTOMATISK,
    ALLEREDE_BEHANDLET,
    UTDATERT_MELDEPERIODE,
    ER_UNDER_REVURDERING,
}

fun BrukersMeldekortBehandletAutomatiskStatus.tilDb(): String = when (this) {
    BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.BEHANDLET
    BrukersMeldekortBehandletAutomatiskStatus.UKJENT_FEIL -> MeldekortBehandletAutomatiskStatusDb.UKJENT_FEIL
    BrukersMeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET -> MeldekortBehandletAutomatiskStatusDb.HENTE_NAVKONTOR_FEILET
    BrukersMeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.BEHANDLING_FEILET_PÅ_SAK
    BrukersMeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDb.UTBETALING_FEILET_PÅ_SAK
    BrukersMeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK -> MeldekortBehandletAutomatiskStatusDb.SKAL_IKKE_BEHANDLES_AUTOMATISK
    BrukersMeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET -> MeldekortBehandletAutomatiskStatusDb.ALLEREDE_BEHANDLET
    BrukersMeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE
    BrukersMeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING -> MeldekortBehandletAutomatiskStatusDb.ER_UNDER_REVURDERING
}.toString()

fun String.tilMeldekortBehandletAutomatiskStatus(): BrukersMeldekortBehandletAutomatiskStatus =
    when (MeldekortBehandletAutomatiskStatusDb.valueOf(this)) {
        MeldekortBehandletAutomatiskStatusDb.BEHANDLET -> BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET
        MeldekortBehandletAutomatiskStatusDb.UKJENT_FEIL -> BrukersMeldekortBehandletAutomatiskStatus.UKJENT_FEIL
        MeldekortBehandletAutomatiskStatusDb.HENTE_NAVKONTOR_FEILET -> BrukersMeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET
        MeldekortBehandletAutomatiskStatusDb.BEHANDLING_FEILET_PÅ_SAK -> BrukersMeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK
        MeldekortBehandletAutomatiskStatusDb.UTBETALING_FEILET_PÅ_SAK -> BrukersMeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK
        MeldekortBehandletAutomatiskStatusDb.SKAL_IKKE_BEHANDLES_AUTOMATISK -> BrukersMeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK
        MeldekortBehandletAutomatiskStatusDb.ALLEREDE_BEHANDLET -> BrukersMeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET
        MeldekortBehandletAutomatiskStatusDb.UTDATERT_MELDEPERIODE -> BrukersMeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE
        MeldekortBehandletAutomatiskStatusDb.ER_UNDER_REVURDERING -> BrukersMeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING
    }
