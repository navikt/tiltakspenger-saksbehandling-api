package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import org.junit.jupiter.api.Test

class MeldekortvedtakRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        val tidspunkt = nå(fixedClock)
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, _, meldekortvedtak, meldekort) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val meldekortvedtakRepo = testDataHelper.meldekortvedtakRepo as MeldekortvedtakPostgresRepo

            // Journalføring
            val oppdatertMedUtbetalingsdata = testDataHelper.sessionFactory.withSession { session ->
                MeldekortvedtakPostgresRepo.hentForSakId(sak.id, session)
            }
            meldekortvedtakRepo.hentDeSomSkalJournalføres() shouldBe oppdatertMedUtbetalingsdata
            meldekortvedtakRepo.markerJournalført(
                vedtakId = meldekortvedtak.id,
                journalpostId = JournalpostId("123"),
                tidspunkt = tidspunkt,
            )
            meldekortvedtakRepo.hentDeSomSkalJournalføres() shouldBe emptyList()
        }
    }
}
