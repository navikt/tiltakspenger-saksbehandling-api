package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeBehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId

private const val LEGG_TILBAKE_BEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/legg-tilbake"

fun Route.leggTilbakeBehandlingRoute(
    auditService: AuditService,
    leggTilbakeBehandlingService: LeggTilbakeBehandlingService,
) {
    val logger = KotlinLogging.logger {}
    post(LEGG_TILBAKE_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$LEGG_TILBAKE_BEHANDLING_PATH' - Fjerner saksbehandler/beslutter fra behandlingen." }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withBehandlingId { behandlingId ->
            val correlationId = call.correlationId()

            leggTilbakeBehandlingService.leggTilbakeBehandling(
                behandlingId,
                saksbehandler,
                correlationId = correlationId,
            ).also {
                auditService.logMedBehandlingId(
                    behandlingId = behandlingId,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.UPDATE,
                    contextMessage = "Saksbehandler fjernes fra behandlingen",
                    correlationId = correlationId,
                )

                call.respond(status = HttpStatusCode.OK, it.tilBehandlingDTO())
            }
        }
    }
}
