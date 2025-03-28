package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.frameldekortapi.mottaMeldekortRoutes
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService
import java.time.Clock

fun Route.meldekortRoutes(
    opprettMeldekortBehandlingService: OpprettMeldekortBehandlingService,
    iverksettMeldekortService: IverksettMeldekortService,
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutningService,
    auditService: AuditService,
    sakService: SakService,
    tokenService: TokenService,
    mottaBrukerutfyltMeldekortService: MottaBrukerutfyltMeldekortService,
    underkjennMeldekortBehandlingService: UnderkjennMeldekortBehandlingService,
    clock: Clock,
) {
    hentMeldekortRoute(sakService, auditService, tokenService, clock)
    iverksettMeldekortRoute(iverksettMeldekortService, auditService, tokenService, clock)
    sendMeldekortTilBeslutterRoute(sendMeldekortTilBeslutterService, auditService, tokenService, clock)
    opprettMeldekortBehandlingRoute(opprettMeldekortBehandlingService, auditService, tokenService, clock)
    mottaMeldekortRoutes(mottaBrukerutfyltMeldekortService)
    underkjennMeldekortBehandlingRoute(underkjennMeldekortBehandlingService, auditService, tokenService)
}
