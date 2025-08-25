package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import org.junit.jupiter.api.Test

class MeldekortVedtakRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        val tidspunkt = nå(fixedClock)
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, meldekort) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val meldekortVedtakRepo = testDataHelper.meldekortVedtakRepo as MeldekortVedtakPostgresRepo
            val meldekortVedtak = meldekort.opprettVedtak(null, fixedClock)
            meldekortVedtakRepo.lagre(meldekortVedtak)

            // Journalføring
            val oppdatertMedUtbetalingsdata = testDataHelper.sessionFactory.withSession { session ->
                MeldekortVedtakPostgresRepo.hentForSakId(sak.id, session)
            }
            meldekortVedtakRepo.hentDeSomSkalJournalføres() shouldBe oppdatertMedUtbetalingsdata
            meldekortVedtakRepo.markerJournalført(
                vedtakId = meldekortVedtak.id,
                journalpostId = JournalpostId("123"),
                tidspunkt = tidspunkt,
            )
            meldekortVedtakRepo.hentDeSomSkalJournalføres() shouldBe emptyList()
        }
    }
}
