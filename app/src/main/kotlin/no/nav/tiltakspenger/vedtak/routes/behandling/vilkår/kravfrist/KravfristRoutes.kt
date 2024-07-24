package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider

fun Route.kravfristRoutes(
    innloggetSaksbehandlerProvider: InnloggetSaksbehandlerProvider,
    behandlingService: BehandlingService,
) {
    hentKravfristRoute(innloggetSaksbehandlerProvider, behandlingService)
}
