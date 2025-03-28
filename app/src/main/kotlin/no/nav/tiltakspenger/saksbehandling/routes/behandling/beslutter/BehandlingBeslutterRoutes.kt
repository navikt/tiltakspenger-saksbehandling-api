package no.nav.tiltakspenger.saksbehandling.routes.behandling.beslutter

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.withBody
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.BehandlingService

private data class BegrunnelseDTO(
    val begrunnelse: String?,
)

fun Route.behandlingBeslutterRoutes(
    behandlingService: BehandlingService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}
    post("$BEHANDLING_PATH/sendtilbake/{behandlingId}") {
        logger.debug { "Mottatt post-request på '$BEHANDLING_PATH/sendtilbake/{behandlingId}' - sender behandling tilbake til saksbehandler" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                call.withBody<BegrunnelseDTO> { body ->
                    val begrunnelse = body.begrunnelse
                    val correlationId = call.correlationId()
                    behandlingService.sendTilbakeTilSaksbehandler(
                        behandlingId = behandlingId,
                        beslutter = saksbehandler,
                        begrunnelse = begrunnelse,
                        correlationId = correlationId,
                    ).fold(
                        {
                            val (status, message) = it.toStatusAndErrorJson()
                            call.respond(status, message)
                        },
                        {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Beslutter underkjenner behandling",
                                correlationId = correlationId,
                            )
                            call.respond(status = HttpStatusCode.OK, message = it.toDTO())
                        },
                    )
                }
            }
        }
    }
}

internal fun KanIkkeUnderkjenne.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeUnderkjenne.ManglerBegrunnelse -> HttpStatusCode.BadRequest to ErrorJson(
        "Begrunnelse må være utfylt",
        "begrunnelse_må_være_utfylt",
    )

    KanIkkeUnderkjenne.MåVæreBeslutter -> HttpStatusCode.BadRequest to ErrorJson(
        "Må ha beslutter rolle",
        "må_ha_beslutter_rolle",
    )
}
