package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.service.TilbakekrevingBehandlingTildelingService
import java.time.Clock

fun Route.tilbakekrevingRoutes(
    auditService: AuditService,
    tilbakekrevingBehandlingTildelingService: TilbakekrevingBehandlingTildelingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    taTilbakekrevingBehandlingRoute(
        auditService = auditService,
        tilbakekrevingBehandlingTildelingService = tilbakekrevingBehandlingTildelingService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    overtaTilbakekrevingBehandlingRoute(
        auditService = auditService,
        tilbakekrevingBehandlingTildelingService = tilbakekrevingBehandlingTildelingService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
    leggTilbakeTilbakekrevingBehandlingRoute(
        auditService = auditService,
        tilbakekrevingBehandlingTildelingService = tilbakekrevingBehandlingTildelingService,
        tilgangskontrollService = tilgangskontrollService,
        clock = clock,
    )
}
