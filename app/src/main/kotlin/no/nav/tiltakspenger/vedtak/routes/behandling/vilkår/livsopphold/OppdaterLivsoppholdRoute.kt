package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.KanIkkeLeggeTilSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandler
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.saksopplysningsperiodeMåVæreLik
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond400BadRequest
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId
import no.nav.tiltakspenger.vedtak.routes.withBody

fun Route.oppdaterLivsoppholdRoute(
    livsoppholdVilkårService: LivsoppholdVilkårService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}

    data class YtelseForPeriode(
        val periode: PeriodeDTO,
        val harYtelse: Boolean,
    )

    data class Body(
        val ytelseForPeriode: YtelseForPeriode,
    ) {
        fun toCommand(
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): LeggTilLivsoppholdSaksopplysningCommand =
            LeggTilLivsoppholdSaksopplysningCommand(
                harYtelseForPeriode =
                LeggTilLivsoppholdSaksopplysningCommand.HarYtelseForPeriode(
                    periode = this.ytelseForPeriode.periode.toDomain(),
                    harYtelse = this.ytelseForPeriode.harYtelse,
                ),
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
            )
    }

    post("$BEHANDLING_PATH/{behandlingId}/vilkar/livsopphold") {
        logger.debug("Mottatt post-request på '$BEHANDLING_PATH/{behandlingId}/vilkar/livsopphold' - oppdaterer vilkår om livsoppholdytelser")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                call.withBody<Body> { body ->
                    val correlationId = call.correlationId()
                    livsoppholdVilkårService
                        .leggTilSaksopplysning(
                            body.toCommand(behandlingId, saksbehandler, correlationId),
                        ).fold(
                            {
                                when (it) {
                                    KanIkkeLeggeTilSaksopplysning.MåVæreSaksbehandler -> call.respond403Forbidden(måVæreSaksbehandler())
                                    KanIkkeLeggeTilSaksopplysning.PeriodenMåVæreLikVurderingsperioden -> call.respond400BadRequest(
                                        saksopplysningsperiodeMåVæreLik(),
                                    )
                                }
                            },
                            {
                                auditService.logMedBehandlingId(
                                    behandlingId = behandlingId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Oppdaterer data om vilkåret livsoppholdytelser",
                                    correlationId = correlationId,
                                )

                                call.respond(
                                    status = HttpStatusCode.Created,
                                    message = it.vilkårssett.livsoppholdVilkår.toDTO(),
                                )
                            },
                        )
                }
            }
        }
    }
}
