package no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingInnvilgelseIverksatt
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettRammevedtak
import org.junit.jupiter.api.Test

class RammevedtakPostgresRepoTest {

    @Test
    fun `henter vedtak for datadeling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (_, rammevedtak, _, _) = testDataHelper.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort()
            testDataHelper.sakRepo.hentSakerTilDatadeling().size shouldBe 1
            testDataHelper.vedtakRepo.hentRammevedtakTilDatadeling().size shouldBe 0
            testDataHelper.sakRepo.markerSendtTilDatadeling(rammevedtak.sakId, nå(testDataHelper.clock))
            testDataHelper.sakRepo.hentSakerTilDatadeling().size shouldBe 0
            testDataHelper.vedtakRepo.hentRammevedtakTilDatadeling() shouldBe listOf(rammevedtak)
        }
    }

    @Test
    fun `henter avslagsvedtak for datadeling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (_, rammevedtak) = testDataHelper.persisterRammevedtakAvslag()
            testDataHelper.sakRepo.hentSakerTilDatadeling().size shouldBe 1
            testDataHelper.vedtakRepo.hentRammevedtakTilDatadeling().size shouldBe 0
            testDataHelper.sakRepo.markerSendtTilDatadeling(rammevedtak.sakId, nå(testDataHelper.clock))
            testDataHelper.sakRepo.hentSakerTilDatadeling().size shouldBe 0
            testDataHelper.vedtakRepo.hentRammevedtakTilDatadeling() shouldBe listOf(rammevedtak)
        }
    }

    @Test
    fun `kan lagre rammevedtak med utbetaling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val clock = TikkendeKlokke()
            val (sak) = testDataHelper.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort(clock = clock)
            val innvilgelsesperiode = Periode(sak.førsteDagSomGirRett!!, sak.sisteDagSomGirRett!!)

            val (oppdatertSak, revurdering) = testDataHelper.persisterRevurderingInnvilgelseIverksatt(
                sak = sak,
                barnetillegg = barnetillegg(
                    periode = innvilgelsesperiode,
                    antallBarn = AntallBarn(2),
                ),
                clock = clock,
            )

            val (sakMedNyttVedtak, vedtak) = oppdatertSak.opprettRammevedtak(revurdering, clock)

            testDataHelper.sessionFactory.withTransactionContext { tx ->
                testDataHelper.behandlingRepo.lagre(vedtak.rammebehandling, tx)
                testDataHelper.vedtakRepo.lagre(vedtak, tx)
                sakMedNyttVedtak.rammevedtaksliste.dropLast(1).forEach {
                    testDataHelper.vedtakRepo.oppdaterOmgjortAv(it.id, it.omgjortAvRammevedtak, tx)
                }
            }
            val finalSak = testDataHelper.sakRepo.hentForSakId(sak.id)!!

            val vedtakFraDb = testDataHelper.vedtakRepo.hentForVedtakId(vedtak.id)
            val utbetalinger = finalSak.utbetalinger

            // Første utbetalingen er meldekortbehandlingen, andre er revurderingen av rammevedtaket
            utbetalinger.size shouldBe 2

            vedtakFraDb shouldBe vedtak

            vedtakFraDb!!.utbetaling!!.id shouldBe utbetalinger.last().id
        }
    }
}
