package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import java.time.Clock

internal const val SAK_PATH = "/sak"

fun Route.sakRoutes(
    sakService: SakService,
    auditService: AuditService,
    avbrytSøknadOgBehandlingService: AvbrytSøknadOgBehandlingService,
    clock: Clock,
) {
    hentSakForFnrRoute(sakService, auditService, clock)
    hentSakForSaksnummerRoute(sakService, auditService, clock)
    hentEllerOpprettSakRoute(sakService)
    // TODO jah: Denne føles litt malplassert.
    avbrytSøknadOgBehandling(auditService, avbrytSøknadOgBehandlingService, clock)
}
