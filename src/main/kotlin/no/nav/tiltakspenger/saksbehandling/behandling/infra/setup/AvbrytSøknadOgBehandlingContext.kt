package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkService
import java.time.Clock

open class AvbrytSøknadOgBehandlingContext(
    sakService: SakService,
    søknadService: SøknadService,
    behandlingService: RammebehandlingService,
    saksstatistikkService: SaksstatistikkService,
    sessionFactory: SessionFactory,
    clock: Clock,
) {
    val avsluttSøknadOgBehandlingService =
        AvbrytSøknadOgBehandlingService(
            sakService = sakService,
            søknadService = søknadService,
            behandlingService = behandlingService,
            saksstatistikkService = saksstatistikkService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
}
