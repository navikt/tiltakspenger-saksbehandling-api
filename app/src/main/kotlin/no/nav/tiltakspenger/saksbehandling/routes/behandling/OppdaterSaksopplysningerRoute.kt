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
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterSaksopplysningerService

fun Route.oppdaterSaksopplysningerRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
) {
    val logger = KotlinLogging.logger {}
    patch("/sak/{sakId}/behandling/{behandlingId}/saksopplysninger") {
        logger.debug { "Mottatt get-request på '/sak/{sakId}/behandling/{behandlingId}/saksopplysninger' - henter saksopplysninger fra registre på nytt og oppdaterer behandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    val correlationId = call.correlationId()
                    oppdaterSaksopplysningerService.oppdaterSaksopplysninger(
                        sakId,
                        behandlingId,
                        saksbehandler,
                        correlationId,
                    ).also {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Oppdaterer saksopplysninger",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, it.toDTO())
                    }
                }
            }
        }
    }
}
