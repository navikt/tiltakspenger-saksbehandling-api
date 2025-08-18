package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId

fun Route.hentBehandlingRoute(
    behandlingService: BehandlingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    // TODO jah: Endre til /sak/{sakId}/behandling/{behandlingId}
    get("$BEHANDLING_PATH/{behandlingId}") {
        logger.debug { "Mottatt get-request på '$BEHANDLING_PATH/{behandlingId}' - henter hele behandlingen" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withBehandlingId { behandlingId ->
            val correlationId = call.correlationId()
            behandlingService.hentBehandling(behandlingId, saksbehandler, correlationId).also {
                auditService.logMedBehandlingId(
                    behandlingId = behandlingId,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.ACCESS,
                    contextMessage = "Henter hele behandlingen",
                    correlationId = correlationId,
                )
                call.respond(status = HttpStatusCode.OK, it.tilBehandlingDTO())
            }
        }
    }
}
