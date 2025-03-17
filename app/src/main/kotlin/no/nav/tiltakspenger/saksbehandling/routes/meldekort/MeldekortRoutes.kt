package no.nav.tiltakspenger.saksbehandling.routes.meldekort

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortKorrigeringService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.frameldekortapi.mottaMeldekortRoutes
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService

fun Route.meldekortRoutes(
    opprettMeldekortBehandlingService: OpprettMeldekortBehandlingService,
    opprettMeldekortKorrigeringService: OpprettMeldekortKorrigeringService,
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
    opprettMeldekortKorrigeringRoute(opprettMeldekortKorrigeringService, auditService, tokenService)
    mottaMeldekortRoutes(mottaBrukerutfyltMeldekortService)
}
