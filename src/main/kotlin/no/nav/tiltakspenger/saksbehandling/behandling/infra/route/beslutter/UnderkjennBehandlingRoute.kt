package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.beslutter

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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.BEHANDLING_PATH
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody

private data class BegrunnelseDTO(
    val begrunnelse: String?,
)

fun Route.underkjennBehandlingRoute(
    tokenService: TokenService,
    auditService: AuditService,
    behandlingService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService,
) {
    val logger = KotlinLogging.logger {}
    // TODO jah: Endre til /sak/{sakId}/behandling/{behandlingId}/underkjenn
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
