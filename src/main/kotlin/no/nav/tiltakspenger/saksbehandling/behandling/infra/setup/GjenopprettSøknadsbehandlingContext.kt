package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopprettSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

/**
 * Motstykket til [AvbrytSøknadOgBehandlingContext].
 * Ligger utenfor [BehandlingOgVedtakContext] fordi tjenesten trenger [SøknadService], som hører til søknadsvertikalen.
 */
open class GjenopprettSøknadsbehandlingContext(
    sakService: SakService,
    søknadService: SøknadService,
    rammebehandlingRepo: RammebehandlingRepo,
    hentSaksopplysingerService: HentSaksopplysingerService,
    statistikkService: StatistikkService,
    sessionFactory: SessionFactory,
    clock: Clock,
) {
    val gjenopprettSøknadsbehandlingService =
        GjenopprettSøknadsbehandlingService(
            sakService = sakService,
            søknadService = søknadService,
            rammebehandlingRepo = rammebehandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
            statistikkService = statistikkService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
}
