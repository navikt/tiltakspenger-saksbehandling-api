package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private const val PATH = "/sak/{sakId}/revurdering/start"

fun Route.startRevurderingRoute(
    startRevurderingService: StartRevurderingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Oppretter en ny revurdering" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBody<StartRevurderingBody> { body ->
                val correlationId = call.correlationId()
                startRevurderingService.startRevurdering(
                    kommando = body.tilKommando(
                        sakId,
                        saksbehandler,
                        correlationId,
                    ),
                ).also { (sak, behandling) ->
                    val behandlingId = behandling.id
                    auditService.logMedSakId(
                        sakId = sakId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.CREATE,
                        contextMessage = "Oppretter revurdering på sak $sakId",
                        correlationId = correlationId,
                        behandlingId = behandlingId,
                    )
                    call.respond(HttpStatusCode.OK, sak.tilBehandlingDTO(behandlingId))
                }
            }
        }
    }
}

private data class StartRevurderingBody(
    val revurderingType: BehandlingResultatDTO,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): StartRevurderingKommando {
        return StartRevurderingKommando(
            sakId = sakId,
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            revurderingType = revurderingType.tilRevurderingType(),
        )
    }
}
