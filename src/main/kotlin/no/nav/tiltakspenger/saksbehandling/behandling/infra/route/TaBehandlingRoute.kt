package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.BEHANDLING_PATH
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil.måVæreSaksbehandlerEllerBeslutter
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId

private const val TA_BEHANDLING_PATH = "$BEHANDLING_PATH/tabehandling/{behandlingId}"

fun Route.taBehandlingRoute(
    tokenService: TokenService,
    auditService: AuditService,
    behandlingService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService,
) {
    val logger = KotlinLogging.logger {}
// TODO jah: Endre til /sak/{sakId}/behandling/{behandlingId}/ta - merk at vi vil ha en egen for overta.
    post(TA_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$TA_BEHANDLING_PATH' - Knytter saksbehandler/beslutter til behandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
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
    }
}
