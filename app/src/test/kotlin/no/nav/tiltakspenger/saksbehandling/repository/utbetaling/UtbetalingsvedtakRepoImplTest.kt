package no.nav.tiltakspenger.saksbehandling.repository.utbetaling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.db.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.april
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.felles.nå
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.opprettUtbetalingsvedtak
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

    @Test
    fun utbetalingsstatus() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, meldekort) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingsvedtakRepo = testDataHelper.utbetalingsvedtakRepo as UtbetalingsvedtakPostgresRepo
            // Utbetaling
            val utbetalingsvedtak = meldekort.opprettUtbetalingsvedtak(sak.saksnummer, sak.fnr, null)
            utbetalingsvedtakRepo.lagre(utbetalingsvedtak)
            utbetalingsvedtakRepo.markerSendtTilUtbetaling(
                vedtakId = utbetalingsvedtak.id,
                tidspunkt = nå(),
                utbetalingsrespons = SendtUtbetaling("myReq", "myRes", 202),
            )

            val expected = listOf(
                UtbetalingDetSkalHentesStatusFor(
                    sakId = utbetalingsvedtak.sakId,
                    vedtakId = utbetalingsvedtak.id,
                    saksnummer = utbetalingsvedtak.saksnummer,
                ),
            )
            testDataHelper.sessionFactory.withSession {
                UtbetalingsvedtakPostgresRepo.hentForSakId(sak.id, it).single().status shouldBe null
            }
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected
            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.IkkePåbegynt,
            )
            testDataHelper.sessionFactory.withSession {
                UtbetalingsvedtakPostgresRepo.hentForSakId(sak.id, it).single().status shouldBe Utbetalingsstatus.IkkePåbegynt
            }
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.SendtTilOppdrag,
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.FeiletMotOppdrag,
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.OkUtenUtbetaling,
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.Ok,
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()
        }
    }
}
