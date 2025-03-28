package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.barnetillegg.infra.route.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.infra.route.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import java.time.LocalDate

internal data class ForhåndsvisBehandlingBody(
    val fritekst: String,
    val virkningsperiode: PeriodeDTO?,
    val stansDato: LocalDate?,
    val valgteHjemler: List<String>?,
    val barnetillegg: List<BarnetilleggPeriodeDTO>?,
) {
    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevKommando {
        val virkningsperiode = virkningsperiode?.toDomain()

        return no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevKommando(
            fritekstTilVedtaksbrev = FritekstTilVedtaksbrev(fritekst),
            sakId = sakId,
            behandlingId = behandlingId,
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            virkingsperiode = virkningsperiode,
            valgteHjemler = valgteHjemler ?: emptyList(),
            stansDato = stansDato,
            barnetillegg = barnetillegg?.tilPeriodisering(virkningsperiode),
        )
    }
}

fun Route.forhåndsvisVedtaksbrevRoute(
    tokenService: TokenService,
    auditService: AuditService,
    forhåndsvisVedtaksbrevService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/forhandsvis") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/behandling/{behandlingId}/forhandsvis' - forhåndsviser vedtaksbrev" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<ForhåndsvisBehandlingBody> { body ->
                        val correlationId = call.correlationId()
                        forhåndsvisVedtaksbrevService.forhåndsvisVedtaksbrev(
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
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "forhåndsviser vedtaksbrev",
                                correlationId = correlationId,
                            )
                            call.respondBytes(it.getContent(), ContentType.Application.Pdf)
                        }
                    }
                }
            }
        }
    }
}
