package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService

fun Route.alderRoutes(
    behandlingService: BehandlingService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    hentAlderRoute(behandlingService, auditService, tokenService)
}
