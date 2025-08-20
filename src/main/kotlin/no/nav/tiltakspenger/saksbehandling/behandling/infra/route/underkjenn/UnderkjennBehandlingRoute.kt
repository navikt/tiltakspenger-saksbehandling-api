package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/underkjenn"

private data class BegrunnelseDTO(
    val begrunnelse: String?,
)

fun Route.underkjennBehandlingRoute(
    auditService: AuditService,
    behandlingService: BehandlingService,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - sender behandling tilbake til saksbehandler" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                call.withBody<BegrunnelseDTO> { body ->
                    val begrunnelse = body.begrunnelse
                    val correlationId = call.correlationId()

                    behandlingService.sendTilbakeTilSaksbehandler(
                        sakId = sakId,
                        behandlingId = behandlingId,
                        beslutter = saksbehandler,
                        begrunnelse = begrunnelse,
                        correlationId = correlationId,
                    ).fold(
                        {
                            val (status, message) = it.toStatusAndErrorJson()
                            call.respond(status, message)
                        },
                        { (sak) ->
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Beslutter underkjenner behandling",
                                correlationId = correlationId,
                            )
                            call.respond(status = HttpStatusCode.OK, message = sak.tilBehandlingDTO(behandlingId))
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeUnderkjenne.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeUnderkjenne.ManglerBegrunnelse -> HttpStatusCode.BadRequest to ErrorJson(
        "Begrunnelse må være utfylt",
        "begrunnelse_må_være_utfylt",
    )
}
