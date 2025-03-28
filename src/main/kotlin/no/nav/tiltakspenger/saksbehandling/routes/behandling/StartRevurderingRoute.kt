package no.nav.tiltakspenger.saksbehandling.routes.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KanIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.StartRevurderingService

private const val PATH = "/sak/{sakId}/revurdering/start"

fun Route.startRevurderingRoute(
    tokenService: TokenService,
    startRevurderingService: StartRevurderingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Oppretter en ny revurdering" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                val correlationId = call.correlationId()
                startRevurderingService.startRevurdering(StartRevurderingKommando(sakId, correlationId, saksbehandler))
                    .fold(
                        {
                            when (it) {
                                is KanIkkeStarteRevurdering.HarIkkeTilgang -> {
                                    call.respond403Forbidden(
                                        ikkeTilgang("Krever en av rollene ${it.kreverEnAvRollene} for å starte en behandling."),
                                    )
                                }

                                else -> {
                                    call.respond500InternalServerError(
                                        ErrorJson(
                                            melding = "Kunne ikke opprette revurdering fordi reasons",
                                            kode = "",
                                        ),
                                    )
                                }
                            }
                        },
                        {
                            val revurderingId = it.second.id
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.CREATE,
                                contextMessage = "Oppretter revurdering på sak $sakId",
                                correlationId = correlationId,
                                behandlingId = revurderingId,
                            )
                            call.respond(HttpStatusCode.OK, it.second.toDTO())
                        },
                    )
            }
        }
    }
}
