package no.nav.tiltakspenger.saksbehandling.infra.repo

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.test.common.TestDatabaseConfig
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import java.time.Clock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import no.nav.tiltakspenger.libs.persistering.test.common.TestDatabaseManager as LibsTestDatabaseManager

internal class TestDatabaseManager(
    config: TestDatabaseConfig = TestDatabaseConfig(),
) {
    private val delegate = LibsTestDatabaseManager(
        config = config,
        idGeneratorsFactory = { IdGenerators() },
    )

    init {
        // Flyway-migreringene lager cluster-globale objekter med check-then-create (bl.a. publication og replication slot i V59).
        // Vi initialiserer derfor begge skjemaene sekvensielt her, slik at migreringene aldri kjører samtidig i samme container under parallellkjøring.
        isolatedTestLock.withLock {
            delegate.sessionFactory
            delegate.dataSource(runIsolated = true)
        }
    }

    val sessionFactory: SessionFactory get() = delegate.sessionFactory

    /**
     * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon.
     * Brukes når man gjør operasjoner på tvers av saker.
     * Testen skal da markeres med [no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest].
     */
    fun withMigratedDbTestDataHelper(
        runIsolated: Boolean = false,
        clock: TikkendeKlokke = TikkendeKlokke(),
        test: (TestDataHelper) -> Unit,
    ) {
        medEventuellIsolasjon(runIsolated) {
            delegate.withMigratedDb(runIsolated = runIsolated, clock = clock) { _, idGenerators, _ ->
                test(TestDataHelper(delegate.dataSource(runIsolated), idGenerators, clock))
            }
        }
    }

    fun withMigratedDb(
        runIsolated: Boolean = false,
        clock: TikkendeKlokke = TikkendeKlokke(),
        test: (SessionFactory, IdGenerators, Clock) -> Unit,
    ) {
        medEventuellIsolasjon(runIsolated) {
            delegate.withMigratedDb(runIsolated = runIsolated, clock = clock, test = test)
        }
    }

    private fun medEventuellIsolasjon(runIsolated: Boolean, block: () -> Unit) {
        if (runIsolated) {
            isolatedTestLock.withLock(block)
        } else {
            block()
        }
    }

    companion object {
        /**
         * JVM-global lås som garanterer at kun én runIsolated-test kjører om gangen, i sin helhet, på tvers av alle [TestDatabaseManager]-instanser.
         * Speiler runner-garantien fra [no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest].
         */
        private val isolatedTestLock = ReentrantLock()
    }
}
