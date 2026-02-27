package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJsonMedData
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilErrorJson

fun Route.iverksettRammebehandlingRoute(
    iverksettRammebehandlingService: IverksettRammebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/iverksett") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/behandling/{behandlingId}/iverksett' - iverksetter behandlingen, oppretter vedtak, evt. genererer meldekort og asynkront sender brev." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                iverksettRammebehandlingService.iverksettRammebehandling(
                    rammebehandlingId = behandlingId,
                    beslutter = saksbehandler,
                    sakId = sakId,
                    correlationId = correlationId,
                ).fold(
                    {
                        when (it) {
                            is KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter -> call.respond400BadRequest(
                                behandlingenEiesAvAnnenSaksbehandler(it.eiesAvBeslutter),
                            )

                            is KanIkkeIverksetteBehandling.SimuleringFeil -> call.respondJson(it.feil.tilSimuleringErrorJson())

                            is KanIkkeIverksetteBehandling.UtbetalingFeil -> call.respondJson(
                                it.feil.tilErrorJson().let { (status, errorJson) ->
                                    status to ErrorJsonMedData(
                                        melding = errorJson.melding,
                                        kode = errorJson.kode,
                                        data = it.sak.tilRammebehandlingDTO(it.behandling.id),
                                    )
                                },
                            )
                        }
                    },
                    { (sak) ->
                        auditService.logMedSakId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Beslutter iverksetter behandling $behandlingId",
                            correlationId = correlationId,
                            sakId = sakId,
                        )
                        call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}
