package no.nav.tiltakspenger.saksbehandling.infra.repo

import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOG = KotlinLogging.logger {}

object DataSourceSetup {
    fun createDatasource(
        jdbcUrl: String,
    ): HikariDataSource {
        LOG.info {
            "Kobler til Postgres. Bruker bare jdbc-urlen i config (+ timeout og maxpools)."
        }

        return HikariDataSource().apply {
            this.jdbcUrl = jdbcUrl
            initializationFailTimeout = 5000
            minimumIdle = 5
            maximumPoolSize = 10
        }.also {
            LOG.info { "Starter migrering" }
            flywayMigrate(it)
            LOG.info { "Migrering utført!" }
        }
    }
}
