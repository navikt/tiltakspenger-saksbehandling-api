package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.avslutt.AvbrytSøknadOgBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import java.time.Clock

internal const val SAK_PATH = "/sak"

fun Route.sakRoutes(
    sakService: SakService,
    auditService: AuditService,
    avbrytSøknadOgBehandlingService: AvbrytSøknadOgBehandlingService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
    personService: PersonService,
) {
    søkFnrSaksnummerOgSakIdRoute(sakService, auditService, clock, tilgangskontrollService)
    hentSakForSaksnummerRoute(sakService, auditService, clock, tilgangskontrollService)
    hentEllerOpprettSakSystembrukerRoute(sakService)
    hentEllerOpprettSakRoute(sakService, auditService, tilgangskontrollService, personService)
    toggleKanSendeHelgForMeldekortSakRoute(sakService, auditService, clock, tilgangskontrollService)
    // TODO jah: Denne føles litt malplassert.
    avbrytSøknadOgBehandling(auditService, avbrytSøknadOgBehandlingService, clock, tilgangskontrollService)
}
