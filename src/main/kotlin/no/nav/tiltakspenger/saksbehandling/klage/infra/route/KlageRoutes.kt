package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.klage.service.StartKlagebehandlingService

fun Route.klagebehandlingRoutes(
    startKlagebehandlingService: StartKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    startKlagebehandlingRoute(
        startKlagebehandlingService = startKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
}
