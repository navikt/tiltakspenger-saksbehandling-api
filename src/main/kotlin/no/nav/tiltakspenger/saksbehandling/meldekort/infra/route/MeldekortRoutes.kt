package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.frameldekortapi.mottaMeldekortRoutes
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AvbrytMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.LeggTilbakeMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.TaMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.OvertaMeldekortBehandlingService
import java.time.Clock

fun Route.meldekortRoutes(
    opprettMeldekortBehandlingService: OpprettMeldekortBehandlingService,
    iverksettMeldekortService: IverksettMeldekortService,
    oppdaterMeldekortService: OppdaterMeldekortService,
    auditService: AuditService,
    sakService: SakService,
    mottaBrukerutfyltMeldekortService: MottaBrukerutfyltMeldekortService,
    underkjennMeldekortBehandlingService: UnderkjennMeldekortBehandlingService,
    overtaMeldekortBehandlingService: OvertaMeldekortBehandlingService,
    taMeldekortBehandlingService: TaMeldekortBehandlingService,
    leggTilbakeMeldekortBehandlingService: LeggTilbakeMeldekortBehandlingService,
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutterService,
    avbrytMeldekortBehandlingService: AvbrytMeldekortBehandlingService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    hentMeldekortRoute(sakService, auditService, clock, tilgangskontrollService)
    iverksettMeldekortRoute(iverksettMeldekortService, auditService, clock, tilgangskontrollService)
    sendMeldekortTilBeslutterRoute(sendMeldekortTilBeslutterService, auditService, clock, tilgangskontrollService)
    oppdaterMeldekortBehandlingRoute(oppdaterMeldekortService, auditService, clock, tilgangskontrollService)
    opprettMeldekortBehandlingRoute(opprettMeldekortBehandlingService, auditService, clock, tilgangskontrollService)
    overtaMeldekortBehandlingRoute(overtaMeldekortBehandlingService, auditService, tilgangskontrollService)
    mottaMeldekortRoutes(mottaBrukerutfyltMeldekortService)
    taMeldekortBehandlingRoute(auditService, taMeldekortBehandlingService, tilgangskontrollService)
    underkjennMeldekortBehandlingRoute(underkjennMeldekortBehandlingService, auditService, tilgangskontrollService)
    leggTilbakeMeldekortBehandlingRoute(auditService, leggTilbakeMeldekortBehandlingService, tilgangskontrollService)
    avbrytMeldekortBehandlingRoute(auditService, avbrytMeldekortBehandlingService, tilgangskontrollService)
}
