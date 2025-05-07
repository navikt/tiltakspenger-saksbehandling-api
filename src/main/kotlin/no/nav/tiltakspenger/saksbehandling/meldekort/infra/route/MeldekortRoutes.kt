package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.frameldekortapi.mottaMeldekortRoutes
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
    tokenService: TokenService,
    mottaBrukerutfyltMeldekortService: MottaBrukerutfyltMeldekortService,
    underkjennMeldekortBehandlingService: UnderkjennMeldekortBehandlingService,
    overtaMeldekortBehandlingService: OvertaMeldekortBehandlingService,
    taMeldekortBehandlingService: TaMeldekortBehandlingService,
    leggTilbakeMeldekortBehandlingService: LeggTilbakeMeldekortBehandlingService,
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutterService,
    clock: Clock,
) {
    hentMeldekortRoute(sakService, auditService, tokenService, clock)
    iverksettMeldekortRoute(iverksettMeldekortService, auditService, tokenService, clock)
    sendMeldekortTilBeslutterRoute(sendMeldekortTilBeslutterService, auditService, tokenService, clock)
    oppdaterMeldekortBehandlingRoute(oppdaterMeldekortService, auditService, tokenService, clock)
    opprettMeldekortBehandlingRoute(opprettMeldekortBehandlingService, auditService, tokenService, clock)
    overtaMeldekortBehandlingRoute(tokenService, overtaMeldekortBehandlingService, auditService)
    mottaMeldekortRoutes(mottaBrukerutfyltMeldekortService)
    taMeldekortBehandlingRoute(tokenService, auditService, taMeldekortBehandlingService)
    underkjennMeldekortBehandlingRoute(underkjennMeldekortBehandlingService, auditService, tokenService)
    leggTilbakeMeldekortBehandlingRoute(tokenService, auditService, leggTilbakeMeldekortBehandlingService)
}
