package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

open class AvbrytSøknadOgBehandlingContext(
    sakService: SakService,
    søknadService: SøknadService,
    behandlingService: BehandlingService,
    statistikkSakService: StatistikkSakService,
    sessionFactory: SessionFactory,
) {
    val avsluttSøknadOgBehandlingService =
        AvbrytSøknadOgBehandlingService(
            sakService = sakService,
            søknadService = søknadService,
            behandlingService = behandlingService,
            statistikkSakService = statistikkSakService,
            sessionFactory = sessionFactory,
        )
}
