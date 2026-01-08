package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.validerJournalpostRoute
import no.nav.tiltakspenger.saksbehandling.søknad.service.StartBehandlingAvManueltRegistrertSøknadService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo

fun Route.søknadRoutes(
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    startBehandlingAvManueltRegistrertSøknadService: StartBehandlingAvManueltRegistrertSøknadService,
    søknadService: SøknadService,
    sakService: SakService,
    validerJournalpostService: ValiderJournalpostService,
    tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    mottaSøknadRoute(søknadService, sakService, tiltaksdeltakerRepo)
    startBehandlingAvManueltRegistrertSøknadRoute(
        auditService,
        tilgangskontrollService,
        startBehandlingAvManueltRegistrertSøknadService,
        tiltaksdeltakerRepo,
    )
    validerJournalpostRoute(validerJournalpostService, tilgangskontrollService)
}
