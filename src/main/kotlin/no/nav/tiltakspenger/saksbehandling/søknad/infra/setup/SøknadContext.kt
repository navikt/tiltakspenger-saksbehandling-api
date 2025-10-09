package no.nav.tiltakspenger.saksbehandling.søknad.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.søknad.service.StartBehandlingAvPapirsøknadService
import java.time.Clock

open class SøknadContext(
    sessionFactory: SessionFactory,
    behandlingRepo: BehandlingRepo,
    hentSaksopplysingerService: HentSaksopplysingerService,
    sakService: SakService,
    statistikkSakRepo: StatistikkSakRepo,
    statistikkSakService: StatistikkSakService,
    clock: Clock,
) {
    open val søknadRepo: SøknadRepo by lazy { SøknadPostgresRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val søknadService: SøknadService by lazy {
        SøknadService(
            søknadRepo,
            sessionFactory,
            sakService,
        )
    }
    val registrerPapirsøknadService: StartBehandlingAvPapirsøknadService by lazy {
        StartBehandlingAvPapirsøknadService(
            sessionFactory = sessionFactory,
            behandlingRepo = behandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
            sakService = sakService,
            statistikkSakRepo = statistikkSakRepo,
            statistikkSakService = statistikkSakService,
            søknadRepo = søknadRepo,
            clock = clock,
        )
    }
}
