package no.nav.tiltakspenger.saksbehandling.infra.repo

import org.flywaydb.core.Flyway

/**
 * Migreringene ligger i `db/migration` og callbacks i `db/callback`, og oppsettet er derfor likt i alle miljøer.
 * Callbacks har egen location fordi `checkFlywayMigrationNames` i build.gradle.kts krever at alt under `db/migration` heter `V<versjon>__<beskrivelse>.sql`.
 * Det fantes tidligere en egen lokal variant som i tillegg leste `db/local-migration`, men de filene ble slettet i desember 2024.
 */
fun flywayMigrate(dataSource: javax.sql.DataSource) {
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .locations("db/migration", "db/callback")
        .dataSource(dataSource)
        .load()
        .migrate()
}
