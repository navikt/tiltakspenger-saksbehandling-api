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
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilSammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.barnetillegg.OppdaterBarnetilleggCommand
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KunneIkkeOppdatereBarnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilStatusOgErrorJson
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO

internal const val BARNETILLEGG_PATH = "/sak/{sakId}/behandling/{behandlingId}/barnetillegg"

fun Route.oppdaterBarnetilleggRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterBarnetilleggService: OppdaterBarnetilleggService,
) {
    val logger = KotlinLogging.logger {}
    patch(BARNETILLEGG_PATH) {
        logger.debug { "Mottatt patch-request på $BARNETILLEGG_PATH - oppdaterer barnetillegg" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<OppdaterBarnetilleggBody> { body ->
                        val correlationId = call.correlationId()
                        val toDomain = body.toDomain(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        )

                        oppdaterBarnetilleggService.oppdaterBarnetillegg(toDomain).fold(
                            ifLeft = {
                                val (status, errorJson) = it.tilStatusOgErrorJson()
                                call.respond(status = status, errorJson)
                            },
                            ifRight = {
                                auditService.logMedBehandlingId(
                                    behandlingId = behandlingId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Oppdaterer barnetillegg",
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

internal fun KunneIkkeOppdatereBarnetillegg.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KunneIkkeOppdatereBarnetillegg.KunneIkkeOppdatereBehandling -> this.valideringsfeil.tilStatusOgErrorJson()
}

data class OppdaterBarnetilleggBody(
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
    val innvilgelsesperiode: PeriodeDTO,
    val barnetillegg: BarnetilleggDTO?,
    val antallDagerPerMeldeperiodeForPerioder: List<AntallDagerPerMeldeperiodeDTO>? = listOf(
        AntallDagerPerMeldeperiodeDTO(
            periode = innvilgelsesperiode,
            antallDagerPerMeldeperiode = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        ),
    ),
) {
    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): OppdaterBarnetilleggCommand {
        val innvilgelsesperiode = innvilgelsesperiode.toDomain()

        return OppdaterBarnetilleggCommand(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            innvilgelsesperiode = innvilgelsesperiode,
            barnetillegg = barnetillegg?.tilBarnetillegg(innvilgelsesperiode),
            tiltaksdeltakelser = valgteTiltaksdeltakelser.map { Pair(it.periode.toDomain(), it.eksternDeltagelseId) },
            antallDagerPerMeldeperiode =
            antallDagerPerMeldeperiodeForPerioder
                ?.map {
                    PeriodeMedVerdi(
                        AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                        it.periode.toDomain(),
                    )
                }
                ?.tilSammenhengendePeriodisering(),
        )
    }
}
