package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/sendtilbeslutning"

fun Route.sendBehandlingTilBeslutningRoute(
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - Sender behandlingen til beslutning" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()

                sendBehandlingTilBeslutningService.sendTilBeslutning(
                    kommando = SendBehandlingTilBeslutningKommando(
                        sakId = sakId,
                        behandlingId = behandlingId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ),
                ).onLeft {
                    val error = it.toErrorJson()
                    call.respond(error.first, error.second)
                }.onRight {
                    auditService.logMedBehandlingId(
                        behandlingId = behandlingId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = when (it) {
                            is Revurdering -> "Sender revurdering til beslutter"
                            is Søknadsbehandling -> "Sender søknadsbehandling til beslutter"
                        },
                        correlationId = correlationId,
                    )
                    call.respond(HttpStatusCode.OK, it.tilBehandlingDTO())
                }
            }
        }
    }
}

private fun KanIkkeSendeTilBeslutter.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler -> HttpStatusCode.BadRequest to Standardfeil.behandlingenEiesAvAnnenSaksbehandler(
        this.eiesAvSaksbehandler,
    )

    KanIkkeSendeTilBeslutter.MåVæreUnderBehandlingEllerAutomatisk -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen må være under behandling eller automatisk for å kunne sendes til beslutning",
        "må_være_under_behandling_eller_automatisk",
    )
}
