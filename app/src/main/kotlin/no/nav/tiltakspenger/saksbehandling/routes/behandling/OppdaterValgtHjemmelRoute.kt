package no.nav.tiltakspenger.saksbehandling.routes.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.withBody
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelType
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.toValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterValgtHjemmelService

private data class ValgtHjemmelHarIkkeRettighetBody(
    val valgteHjemler: List<String>,
    val type: ValgtHjemmelType,
) {
    fun toDomain() = valgteHjemler.map { toValgtHjemmelHarIkkeRettighet(type, it) }
}

fun Route.oppdaterValgtHjemmelRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterValgtHjemmelService: OppdaterValgtHjemmelService,
) {
    val logger = KotlinLogging.logger {}
    patch("/sak/{sakId}/behandling/{behandlingId}/valgt-hjemmel") {
        logger.debug("Mottatt patch-request på '/sak/{sakId}/behandling/{behandlingId}/valgt-hjemmel' - oppdaterer valgt hjemmel")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<ValgtHjemmelHarIkkeRettighetBody> { body ->
                        val correlationId = call.correlationId()
                        oppdaterValgtHjemmelService.oppdaterValgtHjemmel(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            valgtHjemmelHarIkkeRettighet = body.toDomain(),
                        ).also {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Oppdaterer valgt hjemmel",
                                correlationId = correlationId,
                            )
                            call.respond(status = HttpStatusCode.OK, it.toDTO())
                        }
                    }
                }
            }
        }
    }
}
