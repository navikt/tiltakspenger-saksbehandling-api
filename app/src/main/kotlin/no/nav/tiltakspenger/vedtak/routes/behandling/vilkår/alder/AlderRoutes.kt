package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.felles.service.AuditService
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider

fun Route.alderRoutes(
    innloggetSaksbehandlerProvider: InnloggetSaksbehandlerProvider,
    behandlingService: BehandlingService,
    auditService: AuditService,
) {
    hentAlderRoute(innloggetSaksbehandlerProvider, behandlingService, auditService)
}
