package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilSammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toAvslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO

internal const val SEND_TIL_BESLUTNING_PATH = "/sak/{sakId}/behandling/{behandlingId}/sendtilbeslutning"

fun Route.sendSøknadsbehandlingTilBeslutningRoute(
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}
    post(SEND_TIL_BESLUTNING_PATH) {
        logger.debug { "Mottatt post-request på $SEND_TIL_BESLUTNING_PATH - Sender behandlingen til beslutning" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<SøknadsbehandlingTilBeslutningBody> { body ->
                        val correlationId = call.correlationId()

                        sendBehandlingTilBeslutningService.sendSøknadsbehandlingTilBeslutning(
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
                            call.respond(HttpStatusCode.OK, it.tilBehandlingDTO())
                        }
                    }
                }
            }
        }
    }
}

internal fun KanIkkeSendeTilBeslutter.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeSendeTilBeslutter.InnvilgelsesperiodenOverlapperMedUtbetaltPeriode -> HttpStatusCode.BadRequest to ErrorJson(
        "Innvilgelsesperioden overlapper med en eller flere utbetalingsperioder",
        "innvilgelsesperioden_overlapper_med_utbetalingsperiode",
    )

    is KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler -> HttpStatusCode.BadRequest to Standardfeil.behandlingenEiesAvAnnenSaksbehandler(
        this.eiesAvSaksbehandler,
    )

    KanIkkeSendeTilBeslutter.MåVæreUnderBehandlingEllerAutomatisk -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen må være under behandling eller automatisk for å kunne sendes til beslutning",
        "må_være_under_behandling_eller_automatisk",
    )
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "resultat")
@JsonSubTypes(
    JsonSubTypes.Type(value = SøknadsbehandlingTilBeslutningBody.InnvilgelseBody::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(value = SøknadsbehandlingTilBeslutningBody.AvslagBody::class, name = "AVSLAG"),
)
sealed interface SøknadsbehandlingTilBeslutningBody {
    val fritekstTilVedtaksbrev: String?
    val begrunnelseVilkårsvurdering: String?
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>

    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): SendSøknadsbehandlingTilBeslutningKommando

    data class InnvilgelseBody(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        override val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val innvilgelsesperiode: PeriodeDTO,
        val barnetillegg: BarnetilleggDTO?,
        val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO>? = listOf(
            AntallDagerPerMeldeperiodeDTO(
                periode = innvilgelsesperiode,
                antallDagerPerMeldeperiode = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
            ),
        ),
    ) : SøknadsbehandlingTilBeslutningBody {
        override fun toDomain(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): SendSøknadsbehandlingTilBeslutningKommando.Innvilgelse {
            val innvilgelsesperiode = this@InnvilgelseBody.innvilgelsesperiode.toDomain()

            return SendSøknadsbehandlingTilBeslutningKommando.Innvilgelse(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { BegrunnelseVilkårsvurdering(saniter(it)) },
                innvilgelsesperiode = innvilgelsesperiode,
                barnetillegg = barnetillegg?.tilBarnetillegg(innvilgelsesperiode),
                tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                    Pair(it.periode.toDomain(), it.eksternDeltagelseId)
                },
                antallDagerPerMeldeperiode =
                antallDagerPerMeldeperiodeForPerioder?.map {
                    PeriodeMedVerdi(
                        AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                        it.periode.toDomain(),
                    )
                }?.tilSammenhengendePeriodisering(),
            )
        }
    }

    data class AvslagBody(
        override val fritekstTilVedtaksbrev: String?,
        override val begrunnelseVilkårsvurdering: String?,
        override val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>,
    ) : SøknadsbehandlingTilBeslutningBody {
        override fun toDomain(
            sakId: SakId,
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): SendSøknadsbehandlingTilBeslutningKommando {
            return SendSøknadsbehandlingTilBeslutningKommando.Avslag(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { BegrunnelseVilkårsvurdering(saniter(it)) },
                tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                    Pair(it.periode.toDomain(), it.eksternDeltagelseId)
                },
                avslagsgrunner = avslagsgrunner.toAvslagsgrunnlag(),
            )
        }
    }
}
