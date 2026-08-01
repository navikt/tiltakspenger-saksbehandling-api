package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Statusene er utfallene av automatisk meldekortbehandling, og hver av dem krever sin egen feilsituasjon for å oppstå gjennom prodstien.
 * Å konstruere alle nitten ville kostet langt mer enn det gir — vi brukte reell tid på å få fram én av dem (`KAN_IKKE_MELDE_HELG`) — og mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en status ble omdøpt i begge `when`-ene samtidig, og da er lagret data ulesbar uten at noe slår ut.
 *
 * Det testen ikke sier noe om, er om hver enkelt status faktisk kan oppstå.
 * En status ingen prodsti produserer er død kode, og skal fjernes framfor å dekkes.
 */
class MeldekortBehandletAutomatiskStatusDbTest {

    @Test
    fun `statusene lagres med sitt avtalte navn`() {
        MeldekortBehandletAutomatiskStatus.entries.associateWith { it.tilDb() } shouldBe mapOf(
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING to "VENTER_BEHANDLING",
            MeldekortBehandletAutomatiskStatus.BEHANDLET to "BEHANDLET",
            MeldekortBehandletAutomatiskStatus.UKJENT_FEIL to "UKJENT_FEIL",
            MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET to "HENTE_NAVKONTOR_FEILET",
            MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK to "BEHANDLING_FEILET_PÅ_SAK",
            MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK to "UTBETALING_FEILET_PÅ_SAK",
            MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK to "SKAL_IKKE_BEHANDLES_AUTOMATISK",
            MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET to "ALLEREDE_BEHANDLET",
            MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE to "UTDATERT_MELDEPERIODE",
            MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING to "ER_UNDER_REVURDERING",
            MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT to "FOR_MANGE_DAGER_REGISTRERT",
            MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG to "KAN_IKKE_MELDE_HELG",
            MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR to "FOR_MANGE_DAGER_GODKJENT_FRAVÆR",
            MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING to "HAR_ÅPEN_BEHANDLING",
            MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE to "MÅ_BEHANDLE_FØRSTE_KJEDE",
            MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE to "MÅ_BEHANDLE_NESTE_KJEDE",
            MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT to "INGEN_DAGER_GIR_RETT",
            MeldekortBehandletAutomatiskStatus.HAR_JUSTERING to "HAR_JUSTERING",
            MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING to "HAR_FEILUTBETALING",
        )
    }

    @Test
    fun `statusene leses tilbake fra lagret verdi`() {
        MeldekortBehandletAutomatiskStatus.entries.forEach {
            it.tilDb().tilMeldekortBehandletAutomatiskStatus() shouldBe it
        }
    }
}
