package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp

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
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp.LeggTilKvpSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.kvp.KvpVilkårService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandler
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId
import no.nav.tiltakspenger.vedtak.routes.withBody

/** Brukes ikke i MVPen. */
fun Route.oppdaterKvpRoute(
    kvpVilkårService: KvpVilkårService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}

    data class DeltarForPeriode(
        val periode: PeriodeDTO,
        val deltar: Boolean,
    )

    data class Body(
        val ytelseForPeriode: List<DeltarForPeriode>,
    ) {
        fun toCommand(
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler,
            correlationId: CorrelationId,
        ): LeggTilKvpSaksopplysningCommand =
            LeggTilKvpSaksopplysningCommand(
                deltakelseForPeriode =
                this.ytelseForPeriode.map {
                    LeggTilKvpSaksopplysningCommand.DeltakelseForPeriode(
                        periode = it.periode.toDomain(),
                        deltar = it.deltar,
                    )
                },
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
            )
    }
    post("$BEHANDLING_PATH/{behandlingId}/vilkar/kvp") {
        logger.debug("Mottatt post-request på '$BEHANDLING_PATH/{behandlingId}/vilkar/kvp' - oppdaterer vilkår om kvalifikasjonsprogrammet")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                call.withBody<Body> { body ->
                    if (body.ytelseForPeriode.isEmpty()) {
                        throw IllegalArgumentException(
                            "Dersom saksbehandler ønsker å legge til en kvp-saksopplysning må hen spesifisere minst én periode",
                        )
                    }
                    val correlationId = call.correlationId()
                    kvpVilkårService.leggTilSaksopplysning(
                        body.toCommand(behandlingId, saksbehandler, correlationId),
                    ).fold(
                        {
                            call.respond403Forbidden(måVæreSaksbehandler())
                        },
                        {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Oppdaterer data om vilkåret kvalifikasjonsprogrammet",
                                correlationId = correlationId,
                            )
                            call.respond(
                                status = HttpStatusCode.Created,
                                message = it.vilkårssett!!.kvpVilkår.toDTO(),
                            )
                        },
                    )
                }
            }
        }
    }
}
