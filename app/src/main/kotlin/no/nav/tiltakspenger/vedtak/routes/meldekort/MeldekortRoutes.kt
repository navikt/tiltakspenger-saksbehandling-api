package no.nav.tiltakspenger.vedtak.routes.meldekort

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.vedtak.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.vedtak.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.vedtak.meldekort.service.SendMeldekortTilBeslutningService
import no.nav.tiltakspenger.vedtak.routes.meldekort.frameldekortapi.mottaMeldekortRoutes
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.SakService

fun Route.meldekortRoutes(
    opprettMeldekortBehandlingService: OpprettMeldekortBehandlingService,
    iverksettMeldekortService: IverksettMeldekortService,
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutningService,
    auditService: AuditService,
    sakService: SakService,
    tokenService: TokenService,
    mottaBrukerutfyltMeldekortService: MottaBrukerutfyltMeldekortService,
) {
    hentMeldekortRoute(sakService, auditService, tokenService)
    iverksettMeldekortRoute(iverksettMeldekortService, auditService, tokenService)
    sendMeldekortTilBeslutterRoute(sendMeldekortTilBeslutterService, auditService, tokenService)
    opprettMeldekortBehandlingRoute(opprettMeldekortBehandlingService, auditService, tokenService)
    mottaMeldekortRoutes(mottaBrukerutfyltMeldekortService)
}
