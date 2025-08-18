package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler

fun Route.iverksettBehandlingRoute(
    iverksettBehandlingService: IverksettBehandlingService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/iverksett") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/behandling/{behandlingId}/iverksett' - iverksetter behandlingen, oppretter vedtak, evt. genererer meldekort og asynkront sender brev." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    val correlationId = call.correlationId()
                    iverksettBehandlingService.iverksett(behandlingId, saksbehandler, correlationId, sakId).fold(
                        {
                            when (it) {
                                is KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter -> call.respond400BadRequest(behandlingenEiesAvAnnenSaksbehandler(it.eiesAvBeslutter))
                            }
                        },
                        { (sak) ->
                            auditService.logMedSakId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Beslutter iverksetter behandling $behandlingId",
                                correlationId = correlationId,
                                sakId = sakId,
                            )
                            MetricRegister.IVERKSATT_BEHANDLING.inc()
                            call.respond(message = sak.tilBehandlingDTO(behandlingId), status = HttpStatusCode.OK)
                        },
                    )
                }
            }
        }
    }
}
