package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingFormkravService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettKlagebehandlingService

fun Route.klagebehandlingRoutes(
    opprettKlagebehandlingService: OpprettKlagebehandlingService,
    oppdaterKlagebehandlingFormkravService: OppdaterKlagebehandlingFormkravService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    opprettKlagebehandlingRoute(
        opprettKlagebehandlingService = opprettKlagebehandlingService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
    oppdaterKlagebehandlingFormkravRoute(
        oppdaterKlagebehandlingFormkravService = oppdaterKlagebehandlingFormkravService,
        auditService = auditService,
        tilgangskontrollService = tilgangskontrollService,
    )
}
