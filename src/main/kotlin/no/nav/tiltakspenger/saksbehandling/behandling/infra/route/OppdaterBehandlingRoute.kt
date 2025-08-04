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
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBehandlingService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/oppdater"

fun Route.oppdaterBehandlingRoute(
    oppdaterBehandlingService: OppdaterBehandlingService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - saksbehandler har oppdatert en behandling" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<OppdaterBehandlingDTO> { body ->
                        val correlationId = call.correlationId()
                        val kommando = body.tilDomene(
                            saksbehandler = saksbehandler,
                            behandlingId = behandlingId,
                            sakId = sakId,
                            correlationId = correlationId,
                        )

                        oppdaterBehandlingService.oppdater(kommando).fold(
                            ifLeft = {
                                val (status, message) = it.tilStatusOgErrorJson()
                                call.respond(status, message)
                            },
                            ifRight = {
                                auditService.logMedBehandlingId(
                                    behandlingId = behandlingId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Saksbehandler har oppdatert en behandling under behandling",
                                    correlationId = correlationId,
                                )
                                call.respond(status = HttpStatusCode.OK, message = it.tilBehandlingDTO())
                            },
                        )
                    }
                }
            }
        }
    }
}
