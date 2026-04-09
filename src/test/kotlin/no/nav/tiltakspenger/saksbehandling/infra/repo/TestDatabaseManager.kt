package no.nav.tiltakspenger.saksbehandling.infra.repo

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.test.common.TestDatabaseConfig
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import java.time.Clock
import no.nav.tiltakspenger.libs.persistering.test.common.TestDatabaseManager as LibsTestDatabaseManager

internal class TestDatabaseManager(
    config: TestDatabaseConfig = TestDatabaseConfig(),
) {
    private val delegate = LibsTestDatabaseManager(
        config = config,
        idGeneratorsFactory = { IdGenerators() },
    )

    val sessionFactory: SessionFactory get() = delegate.sessionFactory

    /**
     * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
     */
    fun withMigratedDbTestDataHelper(
        runIsolated: Boolean = false,
        clock: TikkendeKlokke = TikkendeKlokke(),
        test: (TestDataHelper) -> Unit,
    ) {
        delegate.withMigratedDb(runIsolated = runIsolated, clock = clock) { _, idGenerators, _ ->
            test(TestDataHelper(delegate.dataSource(runIsolated), idGenerators, clock))
        }
    }

    fun withMigratedDb(
        runIsolated: Boolean = false,
        clock: TikkendeKlokke = TikkendeKlokke(),
        test: (SessionFactory, IdGenerators, Clock) -> Unit,
    ) {
        delegate.withMigratedDb(runIsolated = runIsolated, clock = clock, test = test)
    }
}
