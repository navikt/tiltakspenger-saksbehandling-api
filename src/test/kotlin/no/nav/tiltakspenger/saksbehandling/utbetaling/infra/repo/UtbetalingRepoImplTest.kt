package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.gyldigFnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.httpKlientUventetStatus
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
        withMigratedDb { testDataHelper ->
            val (_, _, meldekortvedtak, _) = testDataHelper.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingRepo = testDataHelper.utbetalingRepo

            val utbetaling = meldekortvedtak.utbetaling

            utbetalingRepo.markerSendtTilUtbetaling(
                utbetalingId = utbetaling.id,
                tidspunkt = tidspunkt,
                utbetalingsrespons = SendtUtbetaling("myReq", "myRes", 202, alleredeMottattTidligere = false),
            )
            utbetalingRepo.hentUtbetalingJson(utbetaling.id) shouldBe "myReq"
        }
    }

    @Test
    fun `kan lagre feil ved utbetaling fra meldekortvedtak`() {
        withMigratedDb { testDataHelper ->
            val (_, _, meldekortvedtak, _) = testDataHelper.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val utbetalingRepo = testDataHelper.utbetalingRepo

            val utbetaling = meldekortvedtak.utbetaling

            utbetalingRepo.lagreFeilResponsFraUtbetaling(
                utbetalingId = utbetaling.id,
                utbetalingsrespons = KunneIkkeUtbetale(
                    request = "myFailedReq",
                    feil = httpKlientUventetStatus(statusCode = 409, body = "myFailedRes"),
                ),
            )
            utbetalingRepo.hentUtbetalingJson(utbetaling.id) shouldBe "myFailedReq"
        }
    }

    /**
     * Rundturen for statusfeltet: [no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo.oppdaterUtbetalingsstatus] skriver statusen, og den leses tilbake på meldekortvedtakets utbetaling.
     * Hvilke statuser som holder utbetalingen i statuskøen er spørringens kontrakt, og asserteres i `UtbetalingAggregatTest`.
     */
    @Test
    fun `oppdaterer og leser tilbake utbetalingsstatus`() {
        withMigratedDb { testDataHelper ->
            val (sak, _, meldekortvedtak, _) = testDataHelper.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
                fnr = gyldigFnr(),
            )
            val utbetalingRepo = testDataHelper.utbetalingRepo
            val utbetaling = meldekortvedtak.utbetaling

            utbetalingRepo.markerSendtTilUtbetaling(
                utbetalingId = utbetaling.id,
                tidspunkt = nå(fixedClock.plus(1, ChronoUnit.MICROS)),
                utbetalingsrespons = SendtUtbetaling(utbetaling.toUtbetalingRequestDTO(null), "myRes", 202, alleredeMottattTidligere = false),
            )

            fun lagretStatus(): Utbetalingsstatus? = testDataHelper.sessionFactory.withSession {
                MeldekortvedtakPostgresRepo.hentForSakId(sak.id, it).single().utbetaling.status
            }

            lagretStatus() shouldBe null

            listOf(
                Utbetalingsstatus.IkkePåbegynt,
                Utbetalingsstatus.SendtTilOppdrag,
                Utbetalingsstatus.FeiletMotOppdrag,
                Utbetalingsstatus.OkUtenUtbetaling,
                Utbetalingsstatus.Ok,
            ).forEachIndexed { indeks, status ->
                utbetalingRepo.oppdaterUtbetalingsstatus(
                    utbetalingId = utbetaling.id,
                    status = status,
                    metadata = Forsøkshistorikk.opprett(
                        forrigeForsøk = nå(fixedClock.plus(indeks.toLong() + 2, ChronoUnit.MICROS)),
                        antallForsøk = indeks.toLong() + 1,
                        clock = fixedClock,
                    ),
                )
                lagretStatus() shouldBe status
            }
        }
    }
}
