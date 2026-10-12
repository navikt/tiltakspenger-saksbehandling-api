package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.validerJournalpostRoute
import no.nav.tiltakspenger.saksbehandling.søknad.service.StartBehandlingAvManueltRegistrertSøknadService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import java.time.Clock

fun Route.søknadRoutes(
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    startBehandlingAvManueltRegistrertSøknadService: StartBehandlingAvManueltRegistrertSøknadService,
    søknadService: SøknadService,
    sakService: SakService,
    validerJournalpostService: ValiderJournalpostService,
    tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    clock: Clock,
) {
    mottaSøknadRoute(søknadService, sakService, tiltaksdeltakerRepo, clock)
    startBehandlingAvManueltRegistrertSøknadRoute(
        auditService,
        tilgangskontrollService,
        startBehandlingAvManueltRegistrertSøknadService,
        sakService,
        tiltaksdeltakerRepo,
    )
    validerJournalpostRoute(validerJournalpostService, tilgangskontrollService)
}
