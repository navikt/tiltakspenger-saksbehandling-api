package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo.Companion.toRammevedtak
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V149__migrer_omgjort_rammevedtak: BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            //val repo = RammevedtakPostgresRepo(sessionFactory)
            tx.withSession { session ->
                hentAlleRammevedtak(session).groupBy { it.sakId }.forEach { sakId, rammevedtaksliste ->
                    val sortert = rammevedtaksliste.sortedBy { it.opprettet }
                    val tidslinje = rammevedtaksliste.toTidslinje()
                    RammevedtakRepo.validerIngenOmgjorteRammevedtak(rammevedtaksliste)
                }
            }
        }
    }
}
private fun hentAlleRammevedtak(
    session: Session,
): List<Rammevedtak> {
    return session.run(queryOf("select * from rammevedtak").map { it.toRammevedtak(session) }.asList)
}
