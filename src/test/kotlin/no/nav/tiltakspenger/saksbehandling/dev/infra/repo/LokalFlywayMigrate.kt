package no.nav.tiltakspenger.saksbehandling.dev.infra.repo

import org.flywaydb.core.Flyway
import javax.sql.DataSource

/**
 * Migrerer tabellene som kun finnes ved lokal kjøring, og kjøres etter [flywayMigrate].
 * Filene ligger i `src/test/resources/db/lokal` og havner derfor aldri i prod-artefaktet.
 * De bor i et eget schema med egen historikktabell, slik at versjonsnumrene ikke kolliderer med `db/migration` og at Flyway uten de lokale filene fortsatt validerer mot samme database.
 * Testenes `TestDatabaseManager` leser kun `db/migration`, så testdatabasen får ikke disse tabellene.
 */
fun lokalFlywayMigrate(dataSource: DataSource) {
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .schemas("lokal")
        .locations("db/lokal")
        .dataSource(dataSource)
        .load()
        .migrate()
}
