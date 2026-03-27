package no.nav.tiltakspenger.saksbehandling.infra.repo

import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.sessionOf
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Clock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.sql.DataSource
import kotlin.concurrent.read
import kotlin.concurrent.write

internal class TestDatabaseManager {
    private val postgres: PostgreSQLContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PostgreSQLContainer("postgres:17-alpine").apply {
            withCommand("postgres", "-c", "wal_level=logical")
            start()
        }
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

    private val idGenerators: IdGenerators by lazy { IdGenerators() }
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)

    private val lock by lazy { ReentrantReadWriteLock() }

    /**
     * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
     */
    fun withMigratedDbTestDataHelper(
        runIsolated: Boolean = false,
        clock: TikkendeKlokke = TikkendeKlokke(),
        test: (TestDataHelper) -> Unit,
    ) {
        if (runIsolated) {
            lock.write {
                cleanDatabase()
                test(TestDataHelper(dataSource, idGenerators, clock))
            }
        } else {
            lock.read {
                test(TestDataHelper(dataSource, idGenerators, clock))
            }
        }
    }

    fun withMigratedDb(
        runIsolated: Boolean = false,
        clock: TikkendeKlokke = TikkendeKlokke(),
        test: (SessionFactory, IdGenerators, Clock) -> Unit,
    ) {
        if (runIsolated) {
            lock.write {
                cleanDatabase()
                test(sessionFactory, idGenerators, clock)
            }
        } else {
            lock.read {
                test(sessionFactory, idGenerators, clock)
            }
        }
    }

    private fun cleanDatabase() {
        sessionOf(dataSource).use { session ->
            session.run(
                sqlQuery(
                    """
                TRUNCATE
                  tiltaksdeltaker_kafka,
                  personhendelse,
                  identhendelse,
                  meldekortvedtak,
                  utbetaling,
                  statistikk_utbetaling,
                  statistikk_stonad,
                  statistikk_sak,
                  meldekort_bruker,
                  meldekortbehandling,
                  meldeperiode,
                  rammevedtak,
                  behandling,
                  klagehendelse,
                  klagevedtak,
                  klagebehandling,
                  sak,
                  søknadstiltak,
                  søknad_barnetillegg,
                  søknad,
                  utbetalingsvedtak,
                  tiltaksdeltaker,
                  tilbakekreving_hendelse,
                  tilbakekreving_behandling
                    """.trimIndent(),
                ).asUpdate,
            )
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
