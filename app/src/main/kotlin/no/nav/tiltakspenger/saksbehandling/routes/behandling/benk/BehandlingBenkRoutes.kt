package no.nav.tiltakspenger.saksbehandling.routes.behandling.benk

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.behandling.BEHANDLINGER_PATH
import no.nav.tiltakspenger.saksbehandling.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.måVæreSaksbehandlerEllerBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.sak.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KanIkkeHenteSaksoversikt
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService

private const val TA_BEHANDLING_PATH = "$BEHANDLING_PATH/tabehandling/{behandlingId}"

fun Route.behandlingBenkRoutes(
    tokenService: TokenService,
    behandlingService: BehandlingService,
    sakService: SakService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}

    get(BEHANDLINGER_PATH) {
        logger.debug("Mottatt get-request på $BEHANDLINGER_PATH for å hente alle behandlinger på benken")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            sakService.hentBenkOversikt(
                saksbehandler = saksbehandler,
                correlationId = call.correlationId(),
            ).fold(
                {
                    when (it) {
                        is KanIkkeHenteSaksoversikt.HarIkkeTilgang -> call.respond403Forbidden(
                            ikkeTilgang("Må ha en av rollene ${it.kreverEnAvRollene} for å hente behandlinger på benken."),
                        )
                    }
                },
                {
                    call.respond(status = HttpStatusCode.OK, it.toDTO())
                },
            )
        }
    }

    post(TA_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$TA_BEHANDLING_PATH' - Knytter saksbehandler/beslutter til behandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()

                behandlingService.taBehandling(behandlingId, saksbehandler, correlationId = correlationId).fold(
                    { call.respond403Forbidden(måVæreSaksbehandlerEllerBeslutter()) },
                    {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler tar behandlingen",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, it.toDTO())
                    },
                )
            }
        }
    }
}
