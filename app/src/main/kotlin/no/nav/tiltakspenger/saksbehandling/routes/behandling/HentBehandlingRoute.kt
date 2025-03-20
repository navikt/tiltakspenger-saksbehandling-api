package no.nav.tiltakspenger.saksbehandling.routes.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.måVæreSaksbehandlerEllerBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.BehandlingService

fun Route.hentBehandlingRoute(
    tokenService: TokenService,
    behandlingService: BehandlingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    get("$BEHANDLING_PATH/{behandlingId}") {
        logger.debug { "Mottatt get-request på '$BEHANDLING_PATH/{behandlingId}' - henter hele behandlingen" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                behandlingService.hentBehandlingForSaksbehandler(behandlingId, saksbehandler, correlationId).fold(
                    {
                        call.respond403Forbidden(måVæreSaksbehandlerEllerBeslutter())
                    },
                    {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "Henter hele behandlingen",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, it.toDTO())
                    },
                )
            }
        }
    }
}
