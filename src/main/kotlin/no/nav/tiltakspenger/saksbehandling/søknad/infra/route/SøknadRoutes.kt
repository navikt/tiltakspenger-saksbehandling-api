package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.søknad.service.RegistrerPapirsøknadService
import java.time.Clock

fun Route.søknadRoutes(
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    registrerPapirsøknadService: RegistrerPapirsøknadService,
    søknadService: SøknadService,
    sakService: SakService,
    clock: Clock,
) {
    mottaSøknadRoute(søknadService, sakService)
    startBehandlingAvPapirsøknadRoute(auditService, tilgangskontrollService, registrerPapirsøknadService)
}
