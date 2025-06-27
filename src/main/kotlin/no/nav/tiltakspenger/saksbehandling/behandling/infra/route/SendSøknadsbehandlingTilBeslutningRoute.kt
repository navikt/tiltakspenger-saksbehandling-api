package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toAvslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO

fun Route.sendSøknadsbehandlingTilBeslutningRoute(
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
}

private data class SøknadsbehandlingTilBeslutningBody(
    val fritekstTilVedtaksbrev: String?,
    val begrunnelseVilkårsvurdering: String?,
    val behandlingsperiode: PeriodeDTO,
    val barnetillegg: BarnetilleggDTO?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
    val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO> = listOf(
        AntallDagerPerMeldeperiodeDTO(
            periode = behandlingsperiode,
            antallDagerPerMeldeperiode = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        ),
    ),
    val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>?,
    val resultat: BehandlingResultatDTO,
) {
    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): SendSøknadsbehandlingTilBeslutningKommando {
        val behandlingsperiode = behandlingsperiode.toDomain()

        return SendSøknadsbehandlingTilBeslutningKommando(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev(saniter(it)) },
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering?.let { BegrunnelseVilkårsvurdering(saniter(it)) },
            behandlingsperiode = behandlingsperiode,
            barnetillegg = barnetillegg?.tilBarnetillegg(behandlingsperiode),
            tiltaksdeltakelser = valgteTiltaksdeltakelser.map {
                Pair(it.periode.toDomain(), it.eksternDeltagelseId)
            },
            antallDagerPerMeldeperiode =
            antallDagerPerMeldeperiodeForPerioder.map {
                PeriodeMedVerdi(AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode), it.periode.toDomain())
            }.tilSammenhengendePeriodisering(),
            avslagsgrunner = avslagsgrunner?.toAvslagsgrunnlag(),
            resultat = when (resultat) {
                BehandlingResultatDTO.INNVILGELSE -> SøknadsbehandlingType.INNVILGELSE
                BehandlingResultatDTO.AVSLAG -> SøknadsbehandlingType.AVSLAG
                BehandlingResultatDTO.STANS,
                BehandlingResultatDTO.REVURDERING_INNVILGELSE,
                -> throw IllegalArgumentException("Ugyldig resultat for søknadsbehandling: $resultat")
            },
        )
    }
}
