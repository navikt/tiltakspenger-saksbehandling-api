@file:Suppress("unused", "ktlint")

package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V113__migrer_meldeperiode_rammevedtak_id : BaseJavaMigration() {
    override fun migrate(context: Context) {
        // Fjerner innmat etter migrering er ferdig.
    }
}
