package no.nav.tiltakspenger.saksbehandling.routes.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil
import no.nav.tiltakspenger.saksbehandling.routes.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.withBody
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeSendeTilBeslutter.MåVæreSaksbehandler
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.SendBehandlingTilBeslutningService

private data class SendTilBeslutningBody(
    val fritekstTilVedtaksbrev: String?,
    val begrunnelseVilkårsvurdering: String?,
    val innvilgelsesperiode: PeriodeDTO,
    val barnetillegg: BarnetilleggDTO?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
) {
    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): SendSøknadsbehandlingTilBeslutningKommando {
        val innvilgelsesperiode = innvilgelsesperiode.toDomain()

        return SendSøknadsbehandlingTilBeslutningKommando(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(it) },
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { BegrunnelseVilkårsvurdering(it) },
            innvilgelsesperiode = innvilgelsesperiode,
            barnetillegg = barnetillegg?.tilBarnetillegg(innvilgelsesperiode),
            tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                Pair(it.periode.toDomain(), it.eksternDeltagelseId)
            },
        )
    }
}

fun Route.sendBehandlingTilBeslutningRoute(
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/sendtilbeslutning") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/behandling/{behandlingId}/sendtilbeslutning' - Sender behandlingen til beslutning" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<SendTilBeslutningBody> { body ->
                        val correlationId = call.correlationId()

                        sendBehandlingTilBeslutningService.sendFørstegangsbehandlingTilBeslutning(
                            kommando = body.toDomain(
                                sakId = sakId,
                                behandlingId = behandlingId,
                                saksbehandler = saksbehandler,
                                correlationId = correlationId,
                            ),
                        ).onLeft {
                            val error = it.toErrorJson()
                            call.respond(error.first, error.second)
                        }.onRight {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Sender behandling til beslutter",
                                correlationId = correlationId,
                            )
                            call.respond(HttpStatusCode.OK, it.toDTO())
                        }
                    }
                }
            }
        }
    }
}

internal fun KanIkkeSendeTilBeslutter.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    MåVæreSaksbehandler -> HttpStatusCode.Forbidden to Standardfeil.måVæreSaksbehandler()
    KanIkkeSendeTilBeslutter.PeriodenOverlapperEllerTilstøterMedAnnenBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Innvilgelsesperioden overlapper/tilstøter med eksisterende perioder på saken",
        "innvilgelsesperiode_overlapper_eller_tilstøter_med_eksisternede_perioder",
    )
}
