package no.nav.tiltakspenger.vedtak.routes.sak

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.SakService

internal const val SAK_PATH = "/sak"

fun Route.sakRoutes(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
    avbrytSøknadOgBehandlingService: AvbrytSøknadOgBehandlingService,
) {
    hentSakForFnrRoute(sakService, auditService, tokenService)
    hentSakForSaksnummerRoute(sakService, auditService, tokenService)
    hentEllerOpprettSakRoute(sakService, tokenService)
    avbrytSøknadOgBehandling(tokenService, auditService, avbrytSøknadOgBehandlingService)
}
