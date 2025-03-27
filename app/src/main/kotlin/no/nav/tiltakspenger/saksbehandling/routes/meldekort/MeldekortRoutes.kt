package no.nav.tiltakspenger.saksbehandling.routes.meldekort

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.frameldekortapi.mottaMeldekortRoutes
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
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
    iverksettMeldekortRoute(iverksettMeldekortService, auditService, tokenService)
    sendMeldekortTilBeslutterRoute(sendMeldekortTilBeslutterService, auditService, tokenService)
    opprettMeldekortBehandlingRoute(opprettMeldekortBehandlingService, auditService, tokenService)
    mottaMeldekortRoutes(mottaBrukerutfyltMeldekortService)
    underkjennMeldekortBehandlingRoute(underkjennMeldekortBehandlingService, auditService, tokenService)
}
