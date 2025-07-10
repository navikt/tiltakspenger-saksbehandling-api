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
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaBehandlingService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId

private const val GJENNOPPTA_BEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/gjenoppta"

fun Route.gjenopptaBehandling(
    tokenService: TokenService,
    auditService: AuditService,
    gjenopptaBehandlingService: GjenopptaBehandlingService,
) {
    val logger = KotlinLogging.logger {}
    post(GJENNOPPTA_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$GJENNOPPTA_BEHANDLING_PATH' - Gjenopptar behandling." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()

                gjenopptaBehandlingService.gjenopptaBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                ).also {
                    auditService.logMedBehandlingId(
                        behandlingId = behandlingId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = "Gjenopptar behandling",
                        correlationId = correlationId,
                    )

                    call.respond(status = HttpStatusCode.OK, it.tilBehandlingDTO())
                }
            }
        }
    }
}
