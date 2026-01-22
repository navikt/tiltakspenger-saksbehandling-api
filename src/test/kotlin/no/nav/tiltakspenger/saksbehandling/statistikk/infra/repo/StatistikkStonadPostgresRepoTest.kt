package no.nav.tiltakspenger.saksbehandling.statistikk.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.genererStønadsstatistikkForRammevedtak
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.tilStatistikk
import org.junit.jupiter.api.Test

class StatistikkStonadPostgresRepoTest {

    @Test
    fun `lagre og hente statistikk for rammevedtak`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.statistikkStønadRepo

            val (_, vedtak) = testDataHelper.persisterIverksattSøknadsbehandling()

            val statistikk = genererStønadsstatistikkForRammevedtak(vedtak)

            repo.lagre(statistikk)

            repo.hentForRammevedtak(vedtak.sakId) shouldBe listOf(statistikk)
        }
    }

    @Test
    fun `lagre og hente statistikk for utbetaling`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.statistikkStønadRepo

            val (_, meldekortvedtak) = testDataHelper.persisterIverksattMeldekortbehandling()

            val statistikk = meldekortvedtak.utbetaling.tilStatistikk(testDataHelper.clock)

            repo.lagre(statistikk)

            repo.hentForUtbetalinger(meldekortvedtak.sakId) shouldBe listOf(statistikk)
        }
    }
}
