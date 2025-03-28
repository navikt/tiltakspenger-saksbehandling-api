package no.nav.tiltakspenger.saksbehandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import org.flywaydb.core.Flyway

private fun flyway(dataSource: javax.sql.DataSource): Flyway =
    when (Configuration.applicationProfile()) {
        Profile.LOCAL -> localFlyway(dataSource)
        else -> gcpFlyway(dataSource)
    }

private fun localFlyway(dataSource: javax.sql.DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .locations("db/migration")
        .dataSource(dataSource)
        .load()

private fun gcpFlyway(dataSource: javax.sql.DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .dataSource(dataSource)
        .load()

fun flywayMigrate(dataSource: javax.sql.DataSource) {
    flyway(dataSource).migrate()
}
