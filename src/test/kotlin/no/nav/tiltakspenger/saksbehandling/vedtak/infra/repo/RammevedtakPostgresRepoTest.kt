package no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakAvslag
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterRammevedtakMedBehandletMeldekort
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
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
}
