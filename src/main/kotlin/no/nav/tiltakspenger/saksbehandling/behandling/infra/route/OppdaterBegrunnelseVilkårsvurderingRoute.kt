package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereBegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private data class BegrunnelseBody(
    val begrunnelse: String,
) {
    fun toDomain() = BegrunnelseVilkårsvurdering(saniter(begrunnelse))
}

private const val BEGRUNNELSE_ROUTE = "/sak/{sakId}/behandling/{behandlingId}/begrunnelse"

fun Route.oppdaterBegrunnelseVilkårsvurderingRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterBegrunnelseVilkårsvurderingService: OppdaterBegrunnelseVilkårsvurderingService,
) {
    val logger = KotlinLogging.logger {}
    patch(BEGRUNNELSE_ROUTE) {
        logger.debug { "Mottatt get-request på '$BEGRUNNELSE_ROUTE' - oppdaterer begrunnelse/vilkårsvurdering" }
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
                        ).fold(
                            ifLeft = {
                                val (status, errorJson) = it.tilStatusOgErrorJson()
                                call.respond(status = status, errorJson)
                            },
                            ifRight = {
                                auditService.logMedBehandlingId(
                                    behandlingId = behandlingId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Oppdaterer begrunnelse/vilkårsvurdering",
                                    correlationId = correlationId,
                                )
                                call.respond(status = HttpStatusCode.OK, it.tilBehandlingDTO())
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun KunneIkkeOppdatereBegrunnelseVilkårsvurdering.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> =
    when (this) {
        is KunneIkkeOppdatereBegrunnelseVilkårsvurdering.KunneIkkeOppdatereBehandling -> this.valideringsfeil.tilStatusOgErrorJson()
    }
