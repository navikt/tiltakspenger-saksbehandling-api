package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.infra.repo.sak.StatistikkSakRepoImpl
import no.nav.tiltakspenger.saksbehandling.statistikk.infra.repo.stønad.StatistikkStønadPostgresRepo
import java.time.Clock

open class StatistikkContext(
    sessionFactory: SessionFactory,
    clock: Clock,
) {
    open val statistikkSakRepo: StatistikkSakRepo by lazy { StatistikkSakRepoImpl(sessionFactory as PostgresSessionFactory) }
    open val statistikkStønadRepo: StatistikkStønadRepo by lazy { StatistikkStønadPostgresRepo(sessionFactory as PostgresSessionFactory, clock) }
}
