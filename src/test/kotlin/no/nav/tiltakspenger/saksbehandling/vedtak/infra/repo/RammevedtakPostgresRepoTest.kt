package no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRevurderingInnvilgelseIverksatt
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.clock
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettVedtak
import org.junit.jupiter.api.Test

class RammevedtakPostgresRepoTest {

    @Test
    fun `henter vedtak for datadeling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val (_, _, rammevedtak) = testDataHelper.persisterRammevedtakMedBehandletMeldekort()
            testDataHelper.vedtakRepo.hentRammevedtakTilDatadeling() shouldBe listOf(rammevedtak)
        }
    }

    @Test
    fun `henter ikke avslagsvedtak for datadeling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            testDataHelper.persisterRammevedtakAvslag()
            testDataHelper.vedtakRepo.hentRammevedtakTilDatadeling() shouldBe emptyList()
        }
    }

    @Test
    fun `kan lagre rammevedtak med utbetaling`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val clock = TikkendeKlokke()
            val (sak) = testDataHelper.persisterRammevedtakMedBehandletMeldekort(clock = clock)
            val innvilgesesperiode = Periode(sak.førsteDagSomGirRett!!, sak.sisteDagSomGirRett!!)

            val (oppdatertSak, revurdering) = testDataHelper.persisterRevurderingInnvilgelseIverksatt(
                sak = sak,
                barnetillegg = barnetillegg(
                    periode = innvilgesesperiode,
                    antallBarn = AntallBarn(2),
                ),
                clock = clock,
            )

            val (_, vedtak) = oppdatertSak.opprettVedtak(revurdering, clock)

            testDataHelper.sessionFactory.withTransactionContext { tx ->
                testDataHelper.behandlingRepo.lagre(vedtak.behandling, tx)
                testDataHelper.vedtakRepo.lagre(vedtak, tx)
            }

            val vedtakFraDb = testDataHelper.vedtakRepo.hentForVedtakId(vedtak.id)
            val utbetalinger = testDataHelper.utbetalingRepo.hentForUtsjekk()

            utbetalinger.size shouldBe 1

            vedtakFraDb shouldBe vedtak

            vedtakFraDb!!.utbetaling!!.id shouldBe utbetalinger.first().id
        }
    }
}
