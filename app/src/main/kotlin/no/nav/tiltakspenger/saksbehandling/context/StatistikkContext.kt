package no.nav.tiltakspenger.saksbehandling.context

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.repository.statistikk.sak.StatistikkSakRepoImpl
import no.nav.tiltakspenger.saksbehandling.repository.statistikk.stønad.StatistikkStønadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkStønadRepo

open class StatistikkContext(
    sessionFactory: SessionFactory,
) {
    open val statistikkSakRepo: StatistikkSakRepo by lazy { StatistikkSakRepoImpl(sessionFactory as PostgresSessionFactory) }
    open val statistikkStønadRepo: StatistikkStønadRepo by lazy { StatistikkStønadPostgresRepo(sessionFactory as PostgresSessionFactory) }
}
