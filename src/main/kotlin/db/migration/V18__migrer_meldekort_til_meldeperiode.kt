@file:Suppress("ClassName", "unused")

package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V18__migrer_meldekort_til_meldeperiode : BaseJavaMigration() {

    @Throws(Exception::class)
    override fun migrate(context: Context) {
        /* Vi trenger ikke denne koden lenger. */
    }
}
