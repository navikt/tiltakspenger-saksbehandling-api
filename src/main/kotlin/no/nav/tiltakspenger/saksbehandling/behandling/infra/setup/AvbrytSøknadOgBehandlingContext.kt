package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService

open class AvbrytSøknadOgBehandlingContext(
    sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    søknadService: no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService,
    behandlingService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService,
    sessionFactory: SessionFactory,
) {
    val avsluttSøknadOgBehandlingService =
        no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingServiceImpl(
            sakService = sakService,
            søknadService = søknadService,
            behandlingService = behandlingService,
            sessionFactory = sessionFactory,
        )
}
