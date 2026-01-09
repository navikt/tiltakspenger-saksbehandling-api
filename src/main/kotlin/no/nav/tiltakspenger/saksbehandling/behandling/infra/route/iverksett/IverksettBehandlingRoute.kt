package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler

fun Route.iverksettBehandlingRoute(
    iverksettBehandlingService: IverksettBehandlingService,
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
                iverksettBehandlingService.iverksettRammebehandling(behandlingId, saksbehandler, sakId).fold(
                    {
                        when (it) {
                            is KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter -> call.respond400BadRequest(
                                behandlingenEiesAvAnnenSaksbehandler(it.eiesAvBeslutter),
                            )

                            KanIkkeIverksetteBehandling.KanIkkeHaUtbetaling -> call.respond400BadRequest(
                                melding = "Behandling med utbetaling kan ikke iverksettes på nåværende tidspunkt",
                                kode = "støtter_ikke_utbetaling",
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
                        MetricRegister.IVERKSATT_BEHANDLING.inc()
                        call.respondJson(value = sak.tilBehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}
