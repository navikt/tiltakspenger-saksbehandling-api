package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.service.TaTilbakekrevingBehandlingService
import java.time.Clock

fun Route.tilbakekrevingRoutes(
    auditService: AuditService,
    taTilbakekrevingBehandlingService: TaTilbakekrevingBehandlingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    taTilbakekrevingBehandlingRoute(
        auditService = auditService,
        taTilbakekrevingBehandlingService = taTilbakekrevingBehandlingService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
}
