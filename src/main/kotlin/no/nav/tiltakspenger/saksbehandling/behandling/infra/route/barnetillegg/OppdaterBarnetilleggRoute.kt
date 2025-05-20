package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBarnetilleggKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private fun BarnetilleggDTO.toDomain(
    sakId: SakId,
    behandlingId: BehandlingId,
    correlationId: CorrelationId,
    saksbehandler: Saksbehandler,
): OppdaterBarnetilleggKommando =
    OppdaterBarnetilleggKommando(
        sakId = sakId,
        behandlingId = behandlingId,
        correlationId = correlationId,
        saksbehandler = saksbehandler,
        begrunnelse = begrunnelse?.let { BegrunnelseVilkårsvurdering(saniter(it)) },
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
        logger.debug { "Mottatt patch-request på '/sak/{sakId}/behandling/{behandlingId}/barnetillegg' - oppdaterer barnetillegg" }
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
