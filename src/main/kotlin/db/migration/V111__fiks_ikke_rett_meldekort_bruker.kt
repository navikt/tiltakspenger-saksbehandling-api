@file:Suppress("unused", "ktlint")

package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V111__fiks_ikke_rett_meldekort_bruker : BaseJavaMigration() {
    override fun migrate(context: Context) {
        //allerede kjørt - trenger ikke innholdet
    }

}
