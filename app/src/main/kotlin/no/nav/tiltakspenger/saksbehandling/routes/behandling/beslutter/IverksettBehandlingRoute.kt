package no.nav.tiltakspenger.saksbehandling.routes.behandling.beslutter

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.måVæreBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.IverksettBehandlingService

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
                                KanIkkeIverksetteBehandling.KunneIkkeOppretteOppgave -> call.respond500InternalServerError(
                                    melding = "Feil under oppretting av oppgave for behandlingen",
                                    kode = "",
                                )
                                KanIkkeIverksetteBehandling.MåVæreBeslutter -> call.respond403Forbidden(måVæreBeslutter())
                            }
                        },
                        {
                            auditService.logMedSakId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Beslutter iverksetter behandling $behandlingId",
                                correlationId = correlationId,
                                sakId = sakId,
                            )
                            call.respond(message = it.toDTO(), status = HttpStatusCode.OK)
                        },
                    )
                }
            }
        }
    }
}
