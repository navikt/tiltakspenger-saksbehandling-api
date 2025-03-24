package no.nav.tiltakspenger.saksbehandling.routes.sak

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import java.time.Clock

internal const val SAK_PATH = "/sak"

fun Route.sakRoutes(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
    avbrytSøknadOgBehandlingService: AvbrytSøknadOgBehandlingService,
    clock: Clock,
) {
    hentSakForFnrRoute(sakService, auditService, tokenService, clock)
    hentSakForSaksnummerRoute(sakService, auditService, tokenService, clock)
    hentEllerOpprettSakRoute(sakService, tokenService)
    avbrytSøknadOgBehandling(tokenService, auditService, avbrytSøknadOgBehandlingService, clock)
}
