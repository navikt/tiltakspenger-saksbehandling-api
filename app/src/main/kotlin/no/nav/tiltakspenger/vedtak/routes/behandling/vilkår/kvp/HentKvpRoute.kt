package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandlerEllerBeslutter
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId

fun Route.hentKvpRoute(
    behandlingService: BehandlingService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}
    get("$BEHANDLING_PATH/{behandlingId}/vilkar/kvp") {
        logger.debug("Mottatt get-request på' $BEHANDLING_PATH/{behandlingId}/vilkar/kvp' - henter vilkår om kvalifikasjonsprogrammet")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                behandlingService.hentBehandlingForSaksbehandler(behandlingId, saksbehandler, correlationId).fold(
                    {
                        call.respond403Forbidden(måVæreSaksbehandlerEllerBeslutter())
                    },
                    {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "Henter vilkår om kvalifikasjonsprogrammet",
                            correlationId = correlationId,
                        )
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = it.vilkårssett.kvpVilkår.toDTO(),
                        )
                    },
                )
            }
        }
    }
}
