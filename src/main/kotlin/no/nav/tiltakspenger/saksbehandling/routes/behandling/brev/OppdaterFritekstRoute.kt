package no.nav.tiltakspenger.saksbehandling.routes.behandling.brev

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
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterFritekstTilVedtaksbrevService

private data class FritekstBody(
    val fritekst: String,
) {
    fun toDomain() = FritekstTilVedtaksbrev(fritekst)
}

fun Route.oppdaterFritekstTilVedtaksbrevRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterFritekstTilVedtaksbrevService: OppdaterFritekstTilVedtaksbrevService,
) {
    val logger = KotlinLogging.logger {}
    patch("/sak/{sakId}/behandling/{behandlingId}/fritekst") {
        logger.debug { "Mottatt get-request på '/sak/{sakId}/behandling/{behandlingId}/fritekst' - oppdaterer fritekst til vedtaksbrev" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<FritekstBody> { body ->
                        val correlationId = call.correlationId()
                        oppdaterFritekstTilVedtaksbrevService.oppdaterFritekstTilVedtaksbrev(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            fritekstTilVedtaksbrev = body.toDomain(),
                        ).also {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Oppdaterer fritekst til vedtaksbrev",
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
