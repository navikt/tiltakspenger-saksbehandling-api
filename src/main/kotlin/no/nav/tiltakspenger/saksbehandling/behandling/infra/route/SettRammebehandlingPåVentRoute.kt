package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock
import java.time.LocalDate

private const val SETT_BEHANDLING_PÅ_VENT_PATH = "/sak/{sakId}/behandling/{behandlingId}/pause"

private data class SettPåVentBody(
    val begrunnelse: String,
    val frist: LocalDate?,
) {
    fun toKommando(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
    ) = SettRammebehandlingPåVentKommando(
        sakId = sakId,
        rammebehandlingId = behandlingId,
        begrunnelse = begrunnelse,
        frist = frist,
        saksbehandler = saksbehandler,
    )
}

fun Route.settRammebehandlingPåVentRoute(
    auditService: AuditService,
    settBehandlingPåVentService: SettRammebehandlingPåVentService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    post(SETT_BEHANDLING_PÅ_VENT_PATH) {
        logger.debug { "Mottatt post-request på '$SETT_BEHANDLING_PÅ_VENT_PATH' - Setter behandling på vent inntil videre." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                call.withBody<SettPåVentBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    settBehandlingPåVentService.settBehandlingPåVent(
                        body.toKommando(sakId, behandlingId, saksbehandler),
                    ).also { (sak) ->
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Setter rammebehandling på vent",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = sak.toSakDTO(clock))
                    }
                }
            }
        }
    }
}
