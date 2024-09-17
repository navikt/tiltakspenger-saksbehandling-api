package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.parameter
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider

fun Route.oppdaterLivsoppholdRoute(
    innloggetSaksbehandlerProvider: InnloggetSaksbehandlerProvider,
    livsoppholdVilkårService: LivsoppholdVilkårService,
    auditService: AuditService,
) {
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
        ): LeggTilLivsoppholdSaksopplysningCommand =
            LeggTilLivsoppholdSaksopplysningCommand(
                harYtelseForPeriode =
                LeggTilLivsoppholdSaksopplysningCommand.HarYtelseForPeriode(
                    periode = this.ytelseForPeriode.periode.toDomain(),
                    harYtelse = this.ytelseForPeriode.harYtelse,
                ),
                årsakTilEndring = null,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            )
    }

    post("$BEHANDLING_PATH/{behandlingId}/vilkar/livsopphold") {
        sikkerlogg.debug("Mottatt request på $BEHANDLING_PATH/{behandlingId}/vilkar/livsopphold")

        val saksbehandler: Saksbehandler = innloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(call)
        val behandlingId = BehandlingId.fromString(call.parameter("behandlingId"))
        val body = call.receive<Body>()

        livsoppholdVilkårService
            .leggTilSaksopplysning(
                body.toCommand(behandlingId, saksbehandler),
            ).fold({
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message =
                    """
                        {
                            "feilmelding": "Perioden til saksopplysningen er forskjellig fra vurderingsperioden"
                        }
                    """.trimIndent(),
                )
            }, {
                auditService.logMedBehandlingId(
                    behandlingId = behandlingId,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.UPDATE,
                    contextMessage = "Oppdaterer data om vilkåret livsoppholdytekser",
                    callId = call.callId,
                )

                call.respond(
                    status = HttpStatusCode.Created,
                    message = it.vilkårssett.livsoppholdVilkår.toDTO(),
                )
            })
    }
}
