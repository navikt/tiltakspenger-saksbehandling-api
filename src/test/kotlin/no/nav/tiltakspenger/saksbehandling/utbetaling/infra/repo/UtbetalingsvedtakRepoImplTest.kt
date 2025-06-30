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
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.opprettUtbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class UtbetalingsvedtakRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        val tidspunkt = nå(fixedClock)
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, meldekort) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingsvedtakRepo = testDataHelper.utbetalingsvedtakRepo as UtbetalingsvedtakPostgresRepo
            val utbetalingsvedtak = meldekort.opprettUtbetalingsvedtak(sak.saksnummer, sak.fnr, null, fixedClock)
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
            val utbetalingsvedtak = meldekort.opprettUtbetalingsvedtak(sak.saksnummer, sak.fnr, null, fixedClock)
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
            val utbetalingsvedtak = meldekort.opprettUtbetalingsvedtak(sak.saksnummer, sak.fnr, null, fixedClock)
            utbetalingsvedtakRepo.lagre(utbetalingsvedtak)
            val sendtTilUtbetalingTidspunkt = nå(fixedClock.plus(1, ChronoUnit.MICROS))
            utbetalingsvedtakRepo.markerSendtTilUtbetaling(
                vedtakId = utbetalingsvedtak.id,
                tidspunkt = sendtTilUtbetalingTidspunkt,
                utbetalingsrespons = SendtUtbetaling("myReq", "myRes", 202),
            )

            fun expected(
                forsøkshistorikk: Forsøkshistorikk? = Forsøkshistorikk(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(1, ChronoUnit.MICROS),
                    antallForsøk = 1,
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
                UtbetalingsvedtakPostgresRepo.hentForSakId(sak.id, it).single().status shouldBe null
            }
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected(forsøkshistorikk = null)
            val forsøk1 = Forsøkshistorikk(
                forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(1, ChronoUnit.MICROS),
                antallForsøk = 1,
            )
            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.IkkePåbegynt,
                metadata = forsøk1,
            )
            testDataHelper.sessionFactory.withSession {
                UtbetalingsvedtakPostgresRepo.hentForSakId(sak.id, it)
                    .single().status shouldBe Utbetalingsstatus.IkkePåbegynt
            }
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected(forsøk1)

            val forsøk2 = Forsøkshistorikk(
                forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(2, ChronoUnit.MICROS),
                antallForsøk = 2,
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
                metadata = Forsøkshistorikk(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(3, ChronoUnit.MICROS),
                    antallForsøk = 3,
                ),
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.OkUtenUtbetaling,
                metadata = Forsøkshistorikk(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(4, ChronoUnit.MICROS),
                    antallForsøk = 4,
                ),
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                vedtakId = utbetalingsvedtak.id,
                status = Utbetalingsstatus.Ok,
                metadata = Forsøkshistorikk(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(5, ChronoUnit.MICROS),
                    antallForsøk = 5,
                ),
            )
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()
        }
    }
}
