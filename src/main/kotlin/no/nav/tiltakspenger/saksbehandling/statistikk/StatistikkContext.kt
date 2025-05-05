package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakRepoImpl
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadPostgresRepo
import java.time.Clock

open class StatistikkContext(
    sessionFactory: SessionFactory,
    tilgangsstyringService: TilgangsstyringService,
    gitHash: String,
    clock: Clock,
) {
    open val statistikkSakRepo: StatistikkSakRepo by lazy { StatistikkSakRepoImpl(sessionFactory as PostgresSessionFactory) }
    open val statistikkStønadRepo: StatistikkStønadRepo by lazy { StatistikkStønadPostgresRepo(sessionFactory as PostgresSessionFactory, clock) }

    val statistikkSakService: StatistikkSakService by lazy {
        StatistikkSakService(
            tilgangsstyringService = tilgangsstyringService,
            gitHash = gitHash,
            clock = clock,
        )
    }
}
