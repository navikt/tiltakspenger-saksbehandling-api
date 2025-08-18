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
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeBehandlingService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private const val LEGG_TILBAKE_BEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/legg-tilbake"

fun Route.leggTilbakeBehandlingRoute(
    tokenService: TokenService,
    auditService: AuditService,
    leggTilbakeBehandlingService: LeggTilbakeBehandlingService,
) {
    val logger = KotlinLogging.logger {}
    post(LEGG_TILBAKE_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$LEGG_TILBAKE_BEHANDLING_PATH' - Fjerner saksbehandler/beslutter fra behandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    val correlationId = call.correlationId()

                    leggTilbakeBehandlingService.leggTilbakeBehandling(
                        sakId,
                        behandlingId,
                        saksbehandler,
                        correlationId = correlationId,
                    ).also { (sak) ->
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler fjernes fra behandlingen",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, sak.tilBehandlingDTO(behandlingId))
                    }
                }
            }
        }
    }
}
