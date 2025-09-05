@file:Suppress("unused", "ktlint")

package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V122__migrer_utbetalinger : BaseJavaMigration() {
    override fun migrate(context: Context) {
        // Fjerner innmat etter migrering er ferdig.
    }
}
