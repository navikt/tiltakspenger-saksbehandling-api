package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

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

                        call.respond(status = HttpStatusCode.OK, it.tilBehandlingDTO())
                    }
                }
            }
        }
    }
}
