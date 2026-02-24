package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeRammebehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendRammebehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilErrorJson

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/sendtilbeslutning"

fun Route.sendRammebehandlingTilBeslutningRoute(
    sendBehandlingTilBeslutningService: SendRammebehandlingTilBeslutningService,
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
                    call.respondJson(valueAndStatus = it.toErrorJson())
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
                    call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                }
            }
        }
    }
}

private fun KanIkkeSendeRammebehandlingTilBeslutter.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeSendeRammebehandlingTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler -> HttpStatusCode.BadRequest to Standardfeil.behandlingenEiesAvAnnenSaksbehandler(
        this.eiesAvSaksbehandler,
    )

    KanIkkeSendeRammebehandlingTilBeslutter.MåVæreUnderBehandlingEllerAutomatisk -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen må være under behandling eller automatisk for å kunne sendes til beslutning",
        "må_være_under_behandling_eller_automatisk",
    )

    KanIkkeSendeRammebehandlingTilBeslutter.ErPaVent -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er satt på vent",
        "behandlingen_er_pa_vent",
    )

    is KanIkkeSendeRammebehandlingTilBeslutter.UtbetalingFeil -> this.feil.tilErrorJson()

    is KanIkkeSendeRammebehandlingTilBeslutter.SimuleringFeil -> this.feil.tilSimuleringErrorJson()
}
