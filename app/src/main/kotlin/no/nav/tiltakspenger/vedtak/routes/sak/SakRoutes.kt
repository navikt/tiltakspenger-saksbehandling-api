package no.nav.tiltakspenger.vedtak.routes.sak

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService

internal const val SAK_PATH = "/sak"

fun Route.sakRoutes(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    hentSakForFnrRoute(sakService, auditService, tokenService)
    hentSakForSaksnummerRoute(sakService, auditService, tokenService)
    hentEllerOpprettSakRoute(sakService, tokenService)
}
