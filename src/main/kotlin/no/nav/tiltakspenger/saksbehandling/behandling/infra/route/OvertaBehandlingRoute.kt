package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaBehandlingService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

internal const val OVERTA_BEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/overta"

data class OvertaBehandlingBody(
    val overtarFra: String,
)

fun Route.overtaBehandlingRoute(
    tokenService: TokenService,
    overtaBehandlingService: OvertaBehandlingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    patch(OVERTA_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$OVERTA_BEHANDLING_PATH' - Tar over behandling" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<OvertaBehandlingBody> { body ->
                        val correlationId = call.correlationId()
                        overtaBehandlingService.overta(
                            OvertaBehandlingCommand(
                                sakId = sakId,
                                behandlingId = behandlingId,
                                saksbehandler = saksbehandler,
                                correlationId = correlationId,
                                overtarFra = body.overtarFra,
                            ),
                        ).fold(
                            {
                                val (status, error) = it.tilStatusOgErrorJson()
                                call.respond(status, error)
                            },
                            {
                                auditService.logMedBehandlingId(
                                    behandlingId = behandlingId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.CREATE,
                                    contextMessage = "Oppretter behandling fra søknad og starter behandlingen",
                                    correlationId = correlationId,
                                )

                                call.respond(HttpStatusCode.OK, it.tilBehandlingDTO())
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun KunneIkkeOvertaBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreVedtattEllerAvbrutt -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingne kan ikke være vedtatt eller avbrutt",
            "behandlingen_kan_ikke_være_vedtatt_eller_avbrutt",
        )

        KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen har ikke en eksiterende saksbehandler å overta fra",
            "behandlingen_har_ikke_eksisterende_saksbehandler",
        )

        KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen har ikke en eksisterende beslutter for å overta fra",
            "behandlingen_har_ikke_eksisterende_beslutter",
        )

        KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBeslutningForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen må først gå til under beslutning for å overta",
            "behandling_må_være_under_beslutning",
        )

        KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBehandlingForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen må først gå til under behandling for å overta",
            "behandling_må_være_under_behandling",
        )

        KunneIkkeOvertaBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme -> HttpStatusCode.BadRequest to ErrorJson(
            "Saksbehandler og beslutter kan ikke være den samme på samme behandling",
            "saksbehandler_og_beslutter_kan_ikke_vær_den_samme",
        )
    }
}
