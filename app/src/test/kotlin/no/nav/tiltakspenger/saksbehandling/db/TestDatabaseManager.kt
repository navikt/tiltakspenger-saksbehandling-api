package no.nav.tiltakspenger.saksbehandling.db

import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.date.getTimeMillis
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.common.TestSaksnummerGenerator
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.sql.DataSource
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class TestDatabaseManager {

    private val log = KotlinLogging.logger {}

    private val postgres: PostgreSQLContainer<Nothing> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PostgreSQLContainer<Nothing>("postgres:17-alpine").apply { start() }
    }

    private val dataSource: HikariDataSource by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HikariDataSource().apply {
            this.jdbcUrl = postgres.jdbcUrl
            this.maximumPoolSize = 100
            this.minimumIdle = 1
            this.idleTimeout = 10001
            this.connectionTimeout = 1000
            this.maxLifetime = 30001
            this.username = postgres.username
            this.password = postgres.password
            initializationFailTimeout = 5000
        }.also {
            migrateDatabase(it)
        }
    }

    private val counter = AtomicInteger(0)

    private val saksnummerGenerator = TestSaksnummerGenerator()

    private val started: Long by lazy { getTimeMillis() }

    @Volatile
    private var isClosed = false
    private val lock = ReentrantReadWriteLock()

    /**
     * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
     */
    fun withMigratedDb(runIsolated: Boolean = false, test: (TestDataHelper) -> Unit) {
        if (isClosed) {
            throw IllegalStateException("The test database is closed.")
        }
        counter.incrementAndGet()
        try {
            if (runIsolated) {
                lock.write {
                    cleanDatabase()
                    test(TestDataHelper(dataSource, saksnummerGenerator))
                }
            } else {
                lock.read {
                    test(TestDataHelper(dataSource, saksnummerGenerator))
                }
            }
        } finally {
            if (getTimeMillis() - started > 10 && counter.get() == 0) {
                close()
            }
        }
        counter.decrementAndGet()
    }

    private fun cleanDatabase() {
        sessionOf(dataSource).run(
            queryOf(
                """
                TRUNCATE
                  tiltaksdeltaker_kafka,
                  utbetalingsvedtak,
                  statistikk_utbetaling,
                  statistikk_stønad,
                  statistikk_sak,
                  statistikk_sak_vilkår,
                  meldekort_bruker,
                  meldekortbehandling,
                  meldeperiode,
                  rammevedtak,
                  behandling,
                  sak,
                  søknadstiltak,
                  søknad_barnetillegg,
                  søknad
                """.trimIndent(),
            ).asUpdate,
        )
    }

    private fun close() {
        if (!isClosed) {
            log.info { "Stenger testdatabasen" }
            dataSource.close()
            postgres.stop()
            isClosed = true
        } else {
            log.info { "Testdatabasen er allerede stengt. Vi gjør ingenting." }
        }
    }

    private fun migrateDatabase(dataSource: DataSource): MigrateResult? {
        return Flyway
            .configure()
            .loggers("slf4j")
            .encoding("UTF-8")
            .locations("db/migration")
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}
