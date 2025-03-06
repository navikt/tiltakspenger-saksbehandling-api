package no.nav.tiltakspenger.vedtak.repository.utbetaling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.felles.april
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.saksbehandling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.utbetaling.domene.opprettUtbetalingsvedtak
import org.junit.jupiter.api.Test

class UtbetalingsvedtakRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        val tidspunkt = nå()
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, meldekort) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingsvedtakRepo = testDataHelper.utbetalingsvedtakRepo as UtbetalingsvedtakPostgresRepo
            val utbetalingsvedtak = meldekort.opprettUtbetalingsvedtak(sak.saksnummer, sak.fnr, null)
            // Utbetaling
            utbetalingsvedtakRepo.lagre(utbetalingsvedtak)
            utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() shouldBe listOf(utbetalingsvedtak)
            utbetalingsvedtakRepo.markerSendtTilUtbetaling(
                vedtakId = utbetalingsvedtak.id,
                tidspunkt = tidspunkt,
                utbetalingsrespons = SendtUtbetaling("myReq", "myRes", 202),
            )
            utbetalingsvedtakRepo.hentUtbetalingJsonForVedtakId(utbetalingsvedtak.id) shouldBe "myReq"
            utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() shouldBe emptyList()

            // Journalføring
            val oppdatertMedUtbetalingsdata = testDataHelper.sessionFactory.withSession { session ->
                UtbetalingsvedtakPostgresRepo.hentForSakId(sak.id, session)
            }
            utbetalingsvedtakRepo.hentDeSomSkalJournalføres() shouldBe oppdatertMedUtbetalingsdata
            utbetalingsvedtakRepo.markerJournalført(
                vedtakId = utbetalingsvedtak.id,
                journalpostId = JournalpostId("123"),
                tidspunkt = tidspunkt,
            )
            utbetalingsvedtakRepo.hentDeSomSkalJournalføres() shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre feil ved utbetaling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, meldekort) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingsvedtakRepo = testDataHelper.utbetalingsvedtakRepo as UtbetalingsvedtakPostgresRepo
            // Utbetaling
            val utbetalingsvedtak = meldekort.opprettUtbetalingsvedtak(sak.saksnummer, sak.fnr, null)
            utbetalingsvedtakRepo.lagre(utbetalingsvedtak)

            utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() shouldBe listOf(utbetalingsvedtak)
            utbetalingsvedtakRepo.lagreFeilResponsFraUtbetaling(
                vedtakId = utbetalingsvedtak.id,
                utbetalingsrespons = KunneIkkeUtbetale("myFailedReq", "myFailedRes", 409),
            )
            utbetalingsvedtakRepo.hentUtbetalingJsonForVedtakId(utbetalingsvedtak.id) shouldBe "myFailedReq"
            utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk() shouldBe listOf(utbetalingsvedtak)
        }
    }
}
