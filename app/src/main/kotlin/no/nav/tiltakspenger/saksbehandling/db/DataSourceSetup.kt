package no.nav.tiltakspenger.saksbehandling.db

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

object DataSourceSetup {
    private const val MAX_POOLS = 5

    fun createDatasource(
        jdbcUrl: String,
    ): HikariDataSource {
        LOG.info {
            "Kobler til Postgres. Bruker bare jdbc-urlen i config (+ timeout og maxpools)."
        }

        return HikariDataSource().apply {
            this.jdbcUrl = jdbcUrl
            initializationFailTimeout = 5000
            maximumPoolSize = MAX_POOLS
        }.also {
            flywayMigrate(it)
        }
    }
}
