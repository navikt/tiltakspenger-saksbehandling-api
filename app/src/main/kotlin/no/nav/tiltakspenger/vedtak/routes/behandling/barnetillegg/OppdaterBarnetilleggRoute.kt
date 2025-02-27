package no.nav.tiltakspenger.vedtak.routes.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import mu.KotlinLogging
import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterBarnetilleggKommando
import no.nav.tiltakspenger.saksbehandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId
import no.nav.tiltakspenger.vedtak.routes.withBody
import no.nav.tiltakspenger.vedtak.routes.withSakId

private data class OppdaterBarnetilleggBody(
    val barnetilleggForPeriode: List<BarnetilleggForPeriode>,
    val begrunnelse: String?,
) {
    data class BarnetilleggForPeriode(
        val periode: PeriodeDTO,
        val antallBarn: Int,
    )

    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): OppdaterBarnetilleggKommando = OppdaterBarnetilleggKommando(
        sakId = sakId,
        behandlingId = behandlingId,
        correlationId = correlationId,
        saksbehandler = saksbehandler,
        barnetillegg = Barnetillegg(
            value = Periodisering(
                barnetilleggForPeriode.map {
                    PeriodeMedVerdi(AntallBarn(it.antallBarn), it.periode.toDomain())
                },
            ),
            begrunnelse = begrunnelse?.let { BegrunnelseVilkårsvurdering(it) },
        ),
    )
}

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
                    call.withBody<OppdaterBarnetilleggBody> { body ->
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
