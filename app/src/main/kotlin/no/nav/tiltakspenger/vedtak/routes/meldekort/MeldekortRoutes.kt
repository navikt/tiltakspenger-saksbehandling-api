package no.nav.tiltakspenger.vedtak.routes.meldekort

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService

fun Route.meldekortRoutes(
    opprettMeldekortBehandlingService: OpprettMeldekortBehandlingService,
    iverksettMeldekortService: IverksettMeldekortService,
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutterService,
    auditService: AuditService,
    sakService: SakService,
    tokenService: TokenService,
) {
    hentMeldekortRoute(sakService, auditService, tokenService)
    iverksettMeldekortRoute(iverksettMeldekortService, auditService, tokenService)
    sendMeldekortTilBeslutterRoute(sendMeldekortTilBeslutterService, auditService, tokenService)
    opprettMeldekortBehandlingRoute(opprettMeldekortBehandlingService, auditService, tokenService)

    // TODO abn: kommenter inn denne når jobben som mottar meldekort skal lages skikkelig
    // mottaMeldekortRoutes()
}
