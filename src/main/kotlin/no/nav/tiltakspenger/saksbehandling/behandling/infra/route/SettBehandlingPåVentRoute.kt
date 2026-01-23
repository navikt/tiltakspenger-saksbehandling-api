package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettBehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private const val SETT_BEHANDLING_PÅ_VENT_PATH = "/sak/{sakId}/behandling/{behandlingId}/pause"

private data class BegrunnelseDTO(
    val begrunnelse: String,
)

fun Route.settBehandlingPåVentRoute(
    auditService: AuditService,
    settBehandlingPåVentService: SettBehandlingPåVentService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(SETT_BEHANDLING_PÅ_VENT_PATH) {
        logger.debug { "Mottatt post-request på '$SETT_BEHANDLING_PÅ_VENT_PATH' - Setter behandling på vent inntil videre." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                call.withBody<BegrunnelseDTO> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    settBehandlingPåVentService.settBehandlingPåVent(
                        sakId = sakId,
                        behandlingId = behandlingId,
                        begrunnelse = body.begrunnelse,
                        saksbehandler = saksbehandler,
                    ).also { (sak) ->
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Setter behandling på vent",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                    }
                }
            }
        }
    }
}
