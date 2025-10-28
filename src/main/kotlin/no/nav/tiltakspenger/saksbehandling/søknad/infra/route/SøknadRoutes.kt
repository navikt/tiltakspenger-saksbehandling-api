package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.validerJournalpostRoute
import no.nav.tiltakspenger.saksbehandling.søknad.service.StartBehandlingAvPapirsøknadService

fun Route.søknadRoutes(
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    startBehandlingAvPapirsøknadService: StartBehandlingAvPapirsøknadService,
    søknadService: SøknadService,
    sakService: SakService,
    validerJournalpostService: ValiderJournalpostService,
) {
    mottaSøknadRoute(søknadService, sakService)
    startBehandlingAvPapirsøknadRoute(auditService, tilgangskontrollService, startBehandlingAvPapirsøknadService)
    validerJournalpostRoute(validerJournalpostService, tilgangskontrollService)
}
