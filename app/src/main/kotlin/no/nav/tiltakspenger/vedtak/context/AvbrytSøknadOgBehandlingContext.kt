package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.vedtak.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.avslutt.AvbrytSøknadOgBehandlingServiceImpl
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.SakService

open class AvbrytSøknadOgBehandlingContext(
    sakService: SakService,
    søknadService: SøknadService,
    behandlingService: BehandlingService,
    sessionFactory: SessionFactory,
) {
    val avsluttSøknadOgBehandlingService = AvbrytSøknadOgBehandlingServiceImpl(
        sakService = sakService,
        søknadService = søknadService,
        behandlingService = behandlingService,
        sessionFactory = sessionFactory,
    )
}
