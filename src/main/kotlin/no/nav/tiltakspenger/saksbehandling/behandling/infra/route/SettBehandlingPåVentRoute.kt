package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettBehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody

private const val SETT_BEHANDLING_PÅ_VENT_PATH = "/sak/{sakId}/behandling/{behandlingId}/pause"

private data class BegrunnelseDTO(
    val begrunnelse: String,
)

fun Route.settBehandlingPåVentRoute(
    tokenService: TokenService,
    auditService: AuditService,
    settBehandlingPåVentService: SettBehandlingPåVentService,
) {
    val logger = KotlinLogging.logger {}
    post(SETT_BEHANDLING_PÅ_VENT_PATH) {
        logger.debug { "Mottatt post-request på '$SETT_BEHANDLING_PÅ_VENT_PATH' - Setter behandling på vent inntil videre." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                call.withBody<BegrunnelseDTO> { body ->
                    val correlationId = call.correlationId()

                    settBehandlingPåVentService.settBehandlingPåVent(
                        behandlingId = behandlingId,
                        begrunnelse = body.begrunnelse,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ).also {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Setter behandling på vent",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, it.tilBehandlingDTO())
                    }
                }
            }
        }
    }
}
