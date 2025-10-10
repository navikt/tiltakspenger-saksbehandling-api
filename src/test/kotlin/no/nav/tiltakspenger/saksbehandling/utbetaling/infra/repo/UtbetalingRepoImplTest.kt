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
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.gyldigFnr
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingRequestDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class UtbetalingRepoImplTest {
    @Test
    fun `kan lagre og hente utbetaling fra meldekortvedtak`() {
        val tidspunkt = nå(fixedClock)
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (_, _, meldekortVedtak, _) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingRepo = testDataHelper.utbetalingRepo

            val utbetaling = meldekortVedtak.utbetaling

            utbetalingRepo.hentForUtsjekk() shouldBe listOf(utbetaling)
            utbetalingRepo.markerSendtTilUtbetaling(
                utbetalingId = utbetaling.id,
                tidspunkt = tidspunkt,
                utbetalingsrespons = SendtUtbetaling("myReq", "myRes", 202),
            )
            utbetalingRepo.hentUtbetalingJson(utbetaling.id) shouldBe "myReq"
            utbetalingRepo.hentForUtsjekk() shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre feil ved utbetaling fra meldekortvedtak`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (_, _, meldekortvedtak, _) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingRepo = testDataHelper.utbetalingRepo

            val utbetaling = meldekortvedtak.utbetaling

            utbetalingRepo.hentForUtsjekk() shouldBe listOf(utbetaling)
            utbetalingRepo.lagreFeilResponsFraUtbetaling(
                utbetalingId = utbetaling.id,
                utbetalingsrespons = KunneIkkeUtbetale("myFailedReq", "myFailedRes", 409),
            )
            utbetalingRepo.hentUtbetalingJson(utbetaling.id) shouldBe "myFailedReq"
            utbetalingRepo.hentForUtsjekk() shouldBe listOf(utbetaling)
        }
    }

    @Test
    fun utbetalingsstatus() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (sak, _, meldekortVedtak, _) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
                fnr = gyldigFnr(),
            )
            val utbetalingRepo = testDataHelper.utbetalingRepo

            val utbetaling = meldekortVedtak.utbetaling

            val sendtTilUtbetalingTidspunkt = nå(fixedClock.plus(1, ChronoUnit.MICROS))
            utbetalingRepo.markerSendtTilUtbetaling(
                utbetalingId = utbetaling.id,
                tidspunkt = sendtTilUtbetalingTidspunkt,
                utbetalingsrespons = SendtUtbetaling(utbetaling.toUtbetalingRequestDTO(null), "myRes", 202),
            )

            fun expected(
                forsøkshistorikk: Forsøkshistorikk = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(1, ChronoUnit.MICROS),
                    antallForsøk = 1,
                    clock = fixedClock,
                ),
            ) = listOf(
                UtbetalingDetSkalHentesStatusFor(
                    utbetalingId = utbetaling.id,
                    sakId = meldekortVedtak.sakId,
                    saksnummer = meldekortVedtak.saksnummer,
                    opprettet = meldekortVedtak.opprettet,
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
            utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor().also {
                it shouldBe expected(forsøkshistorikk = it.first().forsøkshistorikk)
            }
            val forsøk1 = Forsøkshistorikk.opprett(
                forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(1, ChronoUnit.MICROS),
                antallForsøk = 1,
                clock = fixedClock,
            )
            utbetalingRepo.oppdaterUtbetalingsstatus(
                utbetalingId = utbetaling.id,
                status = Utbetalingsstatus.IkkePåbegynt,
                metadata = forsøk1,
            )
            testDataHelper.sessionFactory.withSession {
                MeldekortVedtakPostgresRepo.hentForSakId(sak.id, it)
                    .single().utbetaling.status shouldBe Utbetalingsstatus.IkkePåbegynt
            }
            utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected(forsøk1)

            val forsøk2 = Forsøkshistorikk.opprett(
                forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(2, ChronoUnit.MICROS),
                antallForsøk = 2,
                clock = fixedClock,
            )
            utbetalingRepo.oppdaterUtbetalingsstatus(
                utbetalingId = utbetaling.id,
                status = Utbetalingsstatus.SendtTilOppdrag,
                metadata = forsøk2,
            )
            utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe expected(forsøk2)

            utbetalingRepo.oppdaterUtbetalingsstatus(
                utbetalingId = utbetaling.id,
                status = Utbetalingsstatus.FeiletMotOppdrag,
                metadata = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(3, ChronoUnit.MICROS),
                    antallForsøk = 3,
                    clock = fixedClock,
                ),
            )
            utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingRepo.oppdaterUtbetalingsstatus(
                utbetalingId = utbetaling.id,
                status = Utbetalingsstatus.OkUtenUtbetaling,
                metadata = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(4, ChronoUnit.MICROS),
                    antallForsøk = 4,
                    clock = fixedClock,
                ),
            )
            utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()

            utbetalingRepo.oppdaterUtbetalingsstatus(
                utbetalingId = utbetaling.id,
                status = Utbetalingsstatus.Ok,
                metadata = Forsøkshistorikk.opprett(
                    forrigeForsøk = sendtTilUtbetalingTidspunkt.plus(5, ChronoUnit.MICROS),
                    antallForsøk = 5,
                    clock = fixedClock,
                ),
            )
            utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor() shouldBe emptyList()
        }
    }
}
