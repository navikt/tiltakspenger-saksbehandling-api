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
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService

private data class BegrunnelseBody(
    val begrunnelse: String,
) {
    fun toDomain() = BegrunnelseVilkårsvurdering(begrunnelse)
}

fun Route.oppdaterBegrunnelseVilkårsvurderingRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterBegrunnelseVilkårsvurderingService: OppdaterBegrunnelseVilkårsvurderingService,
) {
    val logger = KotlinLogging.logger {}
    patch("/sak/{sakId}/behandling/{behandlingId}/begrunnelse") {
        logger.debug { "Mottatt get-request på '/sak/{sakId}/behandling/{behandlingId}/begrunnelse' - oppdaterer begrunnelse/vilkårsvurdering" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<BegrunnelseBody> { body ->
                        val correlationId = call.correlationId()
                        oppdaterBegrunnelseVilkårsvurderingService.oppdaterBegrunnelseVilkårsvurdering(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            begrunnelseVilkårsvurdering = body.toDomain(),
                        ).also {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Oppdaterer begrunnelse/vilkårsvurdering",
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
