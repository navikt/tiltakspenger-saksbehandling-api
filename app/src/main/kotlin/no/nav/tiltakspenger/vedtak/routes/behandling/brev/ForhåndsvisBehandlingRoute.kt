package no.nav.tiltakspenger.vedtak.routes.behandling.brev

import io.ktor.http.ContentType
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.service.behandling.brev.ForhåndsvisVedtaksbrevKommando
import no.nav.tiltakspenger.saksbehandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId
import no.nav.tiltakspenger.vedtak.routes.withBody
import no.nav.tiltakspenger.vedtak.routes.withSakId

internal data class ForhåndsvisBehandlingBody(
    val fritekst: String,
    val virkningsperiode: PeriodeDTO?,
    val barnetillegg: List<BarnetilleggPeriodeDTO>,
) {
    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ) = ForhåndsvisVedtaksbrevKommando(
        fritekstTilVedtaksbrev = FritekstTilVedtaksbrev(fritekst),
        sakId = sakId,
        behandlingId = behandlingId,
        correlationId = correlationId,
        saksbehandler = saksbehandler,
        virkingsperiode = virkningsperiode?.toDomain(),
        barnetillegg = Periodisering(
            barnetillegg.map {
                PeriodeMedVerdi(
                    verdi = AntallBarn(it.antallBarn),
                    periode = it.periode.toDomain(),
                )
            },
        ),
    )
}

fun Route.forhåndsvisVedtaksbrevRoute(
    tokenService: TokenService,
    auditService: AuditService,
    forhåndsvisVedtaksbrevService: ForhåndsvisVedtaksbrevService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/forhandsvis") {
        logger.debug("Mottatt post-request på '/sak/{sakId}/behandling/{behandlingId}/forhandsvis' - forhåndsviser vedtaksbrev")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<ForhåndsvisBehandlingBody> { body ->
                        val correlationId = call.correlationId()
                        forhåndsvisVedtaksbrevService.forhåndsvisInnvilgelsesvedtaksbrev(
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
