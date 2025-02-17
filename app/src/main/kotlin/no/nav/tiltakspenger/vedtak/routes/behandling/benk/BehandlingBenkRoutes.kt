package no.nav.tiltakspenger.vedtak.routes.behandling.benk

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.service.sak.KanIkkeHenteSaksoversikt
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.service.sak.StartRevurderingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLINGER_PATH
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandlerEllerBeslutter
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.sak.toDTO

fun Route.behandlingBenkRoutes(
    tokenService: TokenService,
    behandlingService: BehandlingService,
    sakService: SakService,
    auditService: AuditService,
    startRevurderingService: StartRevurderingService,
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

    post("$BEHANDLING_PATH/tabehandling") {
        logger.debug { "Mottatt post-request på '$BEHANDLING_PATH/tabehandling' - Knytter saksbehandler/beslutter til behandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            // TODO post-mvp jah: Kan ikke behandlingId ligge i pathen?
            val behandlingId = BehandlingId.fromString(call.receive<BehandlingIdDTO>().id)
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

    startRevurderingRoute(tokenService, startRevurderingService, auditService)
}
