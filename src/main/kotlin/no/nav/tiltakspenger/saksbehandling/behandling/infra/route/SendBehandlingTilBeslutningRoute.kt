package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/sendtilbeslutning"

fun Route.sendBehandlingTilBeslutningRoute(
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - Sender behandlingen til beslutning" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

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
                }.onRight { (sak, behandling) ->
                    auditService.logMedBehandlingId(
                        behandlingId = behandlingId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = when (behandling) {
                            is Revurdering -> "Sender revurdering til beslutter"
                            is Søknadsbehandling -> "Sender søknadsbehandling til beslutter"
                        },
                        correlationId = correlationId,
                    )
                    call.respond(HttpStatusCode.OK, sak.tilBehandlingDTO(behandlingId))
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

    KanIkkeSendeTilBeslutter.MåHaSimuleringAvUtbetaling -> HttpStatusCode.InternalServerError to ErrorJson(
        "Behandling med utbetaling må simuleres for å kunne sende til beslutning",
        "må_simuleres",
    )
}
