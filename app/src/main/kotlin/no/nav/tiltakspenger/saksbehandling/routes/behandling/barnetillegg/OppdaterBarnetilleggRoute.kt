package no.nav.tiltakspenger.saksbehandling.routes.behandling.barnetillegg

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.withBody
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.OppdaterBarnetilleggKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterBarnetilleggService

private fun BarnetilleggDTO.toDomain(
    sakId: SakId,
    behandlingId: BehandlingId,
    correlationId: CorrelationId,
    saksbehandler: Saksbehandler,
): OppdaterBarnetilleggKommando = OppdaterBarnetilleggKommando(
    sakId = sakId,
    behandlingId = behandlingId,
    correlationId = correlationId,
    saksbehandler = saksbehandler,
    begrunnelse = begrunnelse?.let { BegrunnelseVilkårsvurdering(it) },
    perioder = perioder.map {
        Pair(it.periode.toDomain(), AntallBarn(it.antallBarn))
    },
)

fun Route.oppdaterBarnetilleggRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterBarnetilleggService: OppdaterBarnetilleggService,
) {
    val logger = KotlinLogging.logger {}
    patch("/sak/{sakId}/behandling/{behandlingId}/barnetillegg") {
        logger.debug("Mottatt patch-request på '/sak/{sakId}/behandling/{behandlingId}/barnetillegg' - oppdaterer barnetillegg")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<BarnetilleggDTO> { body ->
                        val correlationId = call.correlationId()
                        oppdaterBarnetilleggService.oppdaterBarnetillegg(
                            body.toDomain(
                                sakId = sakId,
                                behandlingId = behandlingId,
                                saksbehandler = saksbehandler,
                                correlationId = correlationId,
                            ),
                        ).also {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Oppdaterer barnetillegg",
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
