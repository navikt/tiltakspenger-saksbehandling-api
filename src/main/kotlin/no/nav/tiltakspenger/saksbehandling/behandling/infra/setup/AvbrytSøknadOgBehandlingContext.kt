package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

open class AvbrytSøknadOgBehandlingContext(
    sakService: SakService,
    søknadService: SøknadService,
    statistikkService: StatistikkService,
    sessionFactory: SessionFactory,
    clock: Clock,
    rammebehandlingRepo: RammebehandlingRepo,
) {
    val avsluttSøknadOgBehandlingService =
        AvbrytSøknadOgBehandlingService(
            sakService = sakService,
            søknadService = søknadService,
            statistikkService = statistikkService,
            sessionFactory = sessionFactory,
            clock = clock,
            rammebehandlingRepo = rammebehandlingRepo,
        )
}
