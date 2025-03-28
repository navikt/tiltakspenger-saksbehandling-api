package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import java.time.Clock

internal const val SAK_PATH = "/sak"

fun Route.sakRoutes(
    sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    auditService: AuditService,
    tokenService: TokenService,
    avbrytSøknadOgBehandlingService: no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService,
    clock: Clock,
) {
    hentSakForFnrRoute(sakService, auditService, tokenService, clock)
    hentSakForSaksnummerRoute(sakService, auditService, tokenService, clock)
    hentEllerOpprettSakRoute(sakService, tokenService)
    // TODO jah: Denne føles litt malplassert.
    avbrytSøknadOgBehandling(tokenService, auditService, avbrytSøknadOgBehandlingService, clock)
}
