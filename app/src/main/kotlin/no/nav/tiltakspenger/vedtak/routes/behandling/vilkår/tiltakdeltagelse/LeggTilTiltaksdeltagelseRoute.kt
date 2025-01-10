package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.tiltakdeltagelse

import arrow.core.toNonEmptyListOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.KanIkkeLeggeTilSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.LeggTilTiltaksdeltagelseKommando
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.tiltaksdeltagelse.TiltaksdeltagelseVilkårService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandlerEllerBeslutter
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.saksopplysningsperiodeMåVæreLik
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond400BadRequest
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId
import no.nav.tiltakspenger.vedtak.routes.withBody
import no.nav.tiltakspenger.vedtak.routes.withSakId
private data class Body(
    val statusForPeriode: List<StatusForPeriode>,
) {
    data class StatusForPeriode(
        val periode: PeriodeDTO,
        val status: Status,
    ) {
        /**
         * Se [TiltakDeltakerstatus]
         * Legg kun inn etter hvert som det er et behov.
         * */
        enum class Status {
            HarSluttet,
        }
    }

    fun tilKommando(
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
        behandlingId: BehandlingId,
        sakId: SakId,
    ): LeggTilTiltaksdeltagelseKommando {
        return LeggTilTiltaksdeltagelseKommando(
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            behandlingId = behandlingId,
            statusForPeriode = this.statusForPeriode.map {
                LeggTilTiltaksdeltagelseKommando.StatusForPeriode(
                    periode = it.periode.toDomain(),
                    status = when (it.status) {
                        StatusForPeriode.Status.HarSluttet -> TiltakDeltakerstatus.HarSluttet
                    },
                )
            }.toNonEmptyListOrNull()!!,
            sakId = sakId,
        )
    }
}

fun Route.leggTilTiltaksdeltagelseRoute(
    tiltaksdeltagelseVilkårService: TiltaksdeltagelseVilkårService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/vilkar/tiltaksdeltagelse") {
        logger.debug("Mottatt POST-request på '/sak/{sakId}/behandling/{behandlingId}/vilkar/tiltaksdeltagelse' - Legger til vilkår om tiltaksdeltagelse")
        call.withSakId { sakId ->
            call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<Body> { body ->
                        val correlationId = call.correlationId()
                        tiltaksdeltagelseVilkårService.oppdater(
                            body.tilKommando(
                                correlationId = correlationId,
                                saksbehandler = saksbehandler,
                                behandlingId = behandlingId,
                                sakId = sakId,
                            ),
                        ).fold(
                            {
                                when (it) {
                                    KanIkkeLeggeTilSaksopplysning.MåVæreSaksbehandler -> call.respond403Forbidden(måVæreSaksbehandlerEllerBeslutter())
                                    KanIkkeLeggeTilSaksopplysning.PeriodenMåVæreLikVurderingsperioden -> call.respond400BadRequest(saksopplysningsperiodeMåVæreLik())
                                }
                            },
                            {
                                auditService.logMedBehandlingId(
                                    behandlingId = behandlingId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Legger til vilkår om tiltaksdeltagelse på behandling $behandlingId",
                                    correlationId = correlationId,
                                )
                                call.respond(
                                    status = HttpStatusCode.OK,
                                    message = it.vilkårssett.tiltakDeltagelseVilkår.toDTO(),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
