@file:Suppress("unused", "ktlint")

package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V109__ikke_rett_status : BaseJavaMigration() {
    override fun migrate(context: Context) {
        //allerede kjørt - trenger ikke innholdet
    }
}
