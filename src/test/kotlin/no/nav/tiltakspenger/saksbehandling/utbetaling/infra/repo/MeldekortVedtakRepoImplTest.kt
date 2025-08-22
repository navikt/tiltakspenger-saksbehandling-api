package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

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
            // Utbetaling
            meldekortVedtakRepo.opprett(meldekortVedtak)
            meldekortVedtakRepo.hentUtbetalingsvedtakForUtsjekk() shouldBe listOf(meldekortVedtak)
            meldekortVedtakRepo.markerSendtTilUtbetaling(
                vedtakId = meldekortVedtak.id,
                tidspunkt = tidspunkt,
                utbetalingsrespons = SendtUtbetaling("myReq", "myRes", 202),
            )
            meldekortVedtakRepo.hentUtbetalingJsonForVedtakId(meldekortVedtak.id) shouldBe "myReq"
            meldekortVedtakRepo.hentUtbetalingsvedtakForUtsjekk() shouldBe emptyList()

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

    @Test
    fun `kan lagre feil ved utbetaling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, meldekort) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingsvedtakRepo = testDataHelper.meldekortVedtakRepo as MeldekortVedtakPostgresRepo
            // Utbetaling
            val utbetalingsvedtak = meldekort.opprettVedtak(null, fixedClock)
            utbetalingsvedtakRepo.opprett(utbetalingsvedtak)

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
            val utbetalingsvedtakRepo = testDataHelper.meldekortVedtakRepo as MeldekortVedtakPostgresRepo
            // Utbetaling
            val utbetalingsvedtak = meldekort.opprettVedtak(null, fixedClock)
            utbetalingsvedtakRepo.opprett(utbetalingsvedtak)
            val sendtTilUtbetalingTidspunkt = nå(fixedClock.plus(1, ChronoUnit.MICROS))
            utbetalingsvedtakRepo.markerSendtTilUtbetaling(
                vedtakId = utbetalingsvedtak.id,
                tidspunkt = sendtTilUtbetalingTidspunkt,
                utbetalingsrespons = SendtUtbetaling("myReq", "myRes", 202),
            )

            fun expected(
                forsøkshistorikk: Forsøkshistorikk? = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(1, ChronoUnit.MICROS),
                    antallForsøk = 1,
                    clock = fixedClock,
                ),
            ) = listOf(
                UtbetalingDetSkalHentesStatusFor(
                    sakId = utbetalingsvedtak.sakId,
                    vedtakId = utbetalingsvedtak.id,
                    saksnummer = utbetalingsvedtak.saksnummer,
                    opprettet = utbetalingsvedtak.opprettet,
                    sendtTilUtbetalingstidspunkt = sendtTilUtbetalingTidspunkt,
                    forsøkshistorikk = forsøkshistorikk,
                ),
            )
            testDataHelper.sessionFactory.withSession {
                MeldekortVedtakPostgresRepo.hentForSakId(sak.id, it).single().utbetaling.status shouldBe null
            }
            val forsøk0 = Forsøkshistorikk.opprett(
                forrigeForsøk = null,
                antallForsøk = 0,
                clock = fixedClock,
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected(forsøkshistorikk = forsøk0)
            val forsøk1 = Forsøkshistorikk.opprett(
                forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(1, ChronoUnit.MICROS),
                antallForsøk = 1,
                clock = fixedClock,
            )
            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.IkkePåbegynt,
                metadata = forsøk1,
            )
            testDataHelper.sessionFactory.withSession {
                MeldekortVedtakPostgresRepo.hentForSakId(sak.id, it)
                    .single().utbetaling.status shouldBe Utbetalingsstatus.IkkePåbegynt
            }
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected(forsøk1)

            val forsøk2 = Forsøkshistorikk.opprett(
                forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(2, ChronoUnit.MICROS),
                antallForsøk = 2,
                clock = fixedClock,
            )
            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.SendtTilOppdrag,
                metadata = forsøk2,
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected(forsøk2)

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.FeiletMotOppdrag,
                metadata = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(3, ChronoUnit.MICROS),
                    antallForsøk = 3,
                    clock = fixedClock,
                ),
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.OkUtenUtbetaling,
                metadata = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(4, ChronoUnit.MICROS),
                    antallForsøk = 4,
                    clock = fixedClock,
                ),
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.Ok,
                metadata = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(5, ChronoUnit.MICROS),
                    antallForsøk = 5,
                    clock = fixedClock,
                ),
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()
        }
    }
}
