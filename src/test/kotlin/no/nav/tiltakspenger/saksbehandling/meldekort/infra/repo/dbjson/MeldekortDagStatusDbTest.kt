package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Dagstatusene lagres i jsonb-en på meldekortbehandlingen, med én gren per status i hver retning.
 * Ett meldekort har fjorten dager, og flere av statusene settes kun av saksbehandler i egne situasjoner, så det ville tatt flere behandlinger å nå alle grenene gjennom prodstien — for en mapping som ikke rører postgres.
 *
 * Testen pinner **de faktiske navnene som havner i jsonb-en**, ikke bare rundturen, slik at en omdøpt variant ikke kan slippe gjennom ved at begge `when`-ene endres samtidig.
 */
class MeldekortDagStatusDbTest {

    @Test
    fun `dagstatusene lagres med sitt avtalte navn`() {
        MeldekortDagStatus.entries.associateWith { it.toDb().name } shouldBe mapOf(
            MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET to "DELTATT_UTEN_LØNN_I_TILTAKET",
            MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET to "DELTATT_MED_LØNN_I_TILTAKET",
            MeldekortDagStatus.FRAVÆR_SYK to "FRAVÆR_SYK",
            MeldekortDagStatus.FRAVÆR_SYKT_BARN to "FRAVÆR_SYKT_BARN",
            MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU to "FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU",
            MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV to "FRAVÆR_GODKJENT_AV_NAV",
            MeldekortDagStatus.FRAVÆR_ANNET to "FRAVÆR_ANNET",
            MeldekortDagStatus.IKKE_BESVART to "IKKE_BESVART",
            MeldekortDagStatus.IKKE_TILTAKSDAG to "IKKE_TILTAKSDAG",
            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER to "IKKE_RETT_TIL_TILTAKSPENGER",
        )
    }

    @Test
    fun `dagstatusene leses tilbake fra lagret navn`() {
        MeldekortDagStatusDb.entries.forEach {
            it.toMeldekortDagStatus().toDb() shouldBe it
        }
    }
}
