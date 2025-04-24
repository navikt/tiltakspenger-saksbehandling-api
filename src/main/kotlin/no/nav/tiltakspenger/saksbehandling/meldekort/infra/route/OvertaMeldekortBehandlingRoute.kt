package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

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
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldekortBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.OvertaMeldekortBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.OvertaMeldekortBehandlingService

internal const val OVERTA_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/overta"

data class OvertaBehandlingBody(
    val overtarFra: String,
)

fun Route.overtaMeldekortBehandlingRoute(
    tokenService: TokenService,
    overtaMeldekortBehandlingService: OvertaMeldekortBehandlingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    patch(OVERTA_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$OVERTA_MELDEKORTBEHANDLING_PATH' - Tar over meldekortbehandling" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    call.withBody<OvertaBehandlingBody> { body ->
                        val correlationId = call.correlationId()
                        overtaMeldekortBehandlingService.overta(
                            OvertaMeldekortBehandlingCommand(
                                sakId = sakId,
                                meldekortId = meldekortId,
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
                                auditService.logMedMeldekortId(
                                    meldekortId = meldekortId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Overtar meldekortbehandlingen",
                                    correlationId = correlationId,
                                )

                                call.respond(HttpStatusCode.OK, it.toMeldekortBehandlingDTO(UtbetalingsstatusDTO.IKKE_GODKJENT))
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun KunneIkkeOvertaMeldekortBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KunneIkkeOvertaMeldekortBehandling.BehandlingenKanIkkeVæreGodkjentEllerIkkeRett -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen kan ikke være godkjent eller ikke rett på tiltakspenger",
            "behandlingen_kan_ikke_være_godkjent_eller_ikke_rett",
        )

        KunneIkkeOvertaMeldekortBehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen har ikke en eksisterende beslutter å overta fra",
            "behandlingen_har_ikke_eksisterende_beslutter",
        )

        KunneIkkeOvertaMeldekortBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme -> HttpStatusCode.BadRequest to ErrorJson(
            "Saksbehandler og beslutter kan ikke være den samme på samme behandling",
            "saksbehandler_og_beslutter_kan_ikke_vær_den_samme",
        )

        KunneIkkeOvertaMeldekortBehandling.BehandlingenMåVæreUnderBeslutningForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen må være under beslutning for å overta",
            "behandling_må_være_under_beslutning",
        )
    }
}
