package no.nav.tiltakspenger.vedtak.routes.behandling.tilbeslutter

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandler
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId

fun Route.sendBehandlingTilBeslutterRoute(
    tokenService: TokenService,
    behandlingService: BehandlingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    post("$BEHANDLING_PATH/beslutter/{behandlingId}") {
        logger.debug("Mottatt post-request på '$BEHANDLING_PATH/beslutter/{behandlingId}' - sender behandling til beslutter")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                behandlingService.sendTilBeslutter(behandlingId, saksbehandler, correlationId).fold(
                    { call.respond403Forbidden(måVæreSaksbehandler()) },
                    {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Sender behandlingen til beslutter",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, message = "{}")
                    },
                )
            }
        }
    }
}
