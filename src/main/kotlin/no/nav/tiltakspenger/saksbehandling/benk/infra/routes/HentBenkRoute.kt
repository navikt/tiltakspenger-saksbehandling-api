package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.BEHANDLINGER_PATH
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkOversiktService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId

fun Route.hentBenkRoute(
    tokenService: TokenService,
    benkOversiktService: BenkOversiktService,
) {
    val logger = KotlinLogging.logger {}

    get(BEHANDLINGER_PATH) {
        logger.debug { "Mottatt get-request på $BEHANDLINGER_PATH for å hente alle behandlinger på benken" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            benkOversiktService.hentBenkOversikt(
                saksbehandler = saksbehandler,
                correlationId = call.correlationId(),
            ).also {
                call.respond(status = HttpStatusCode.OK, it.toDTO())
            }
        }
    }
}

private fun List<Behandlingssammendrag>.toDTO(): List<BehandlingssammendragDTO> = this.map { it.toDTO() }

private fun Behandlingssammendrag.toDTO() = BehandlingssammendragDTO(
    sakId = sakId.toString(),
    fnr = fnr.verdi,
    saksnummer = saksnummer.verdi,
    startet = startet.toString(),
    kravtidspunkt = kravtidspunkt?.toString(),
    behandlingstype = behandlingstype.toDTO(),
    status = status?.toBehandlingssammendragStatusDto(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,
)

private fun BehandlingssammendragStatus.toBehandlingssammendragStatusDto(): BehandlingssammendragStatusDto = when (this) {
    BehandlingssammendragStatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatusDto.KLAR_TIL_BEHANDLING
    BehandlingssammendragStatus.UNDER_BEHANDLING -> BehandlingssammendragStatusDto.UNDER_BEHANDLING
    BehandlingssammendragStatus.KLAR_TIL_BESLUTNING -> BehandlingssammendragStatusDto.KLAR_TIL_BESLUTNING
    BehandlingssammendragStatus.UNDER_BESLUTNING -> BehandlingssammendragStatusDto.UNDER_BESLUTNING
    BehandlingssammendragStatus.KLAR_TIL_UTFYLLING -> BehandlingssammendragStatusDto.KLAR_TIL_UTFYLLING
}

private fun BehandlingssammendragType.toDTO(): BehandlingssammendragTypeDTO = when (this) {
    BehandlingssammendragType.SØKNADSBEHANDLING -> BehandlingssammendragTypeDTO.SØKNADSBEHANDLING
    BehandlingssammendragType.REVURDERING -> BehandlingssammendragTypeDTO.REVURDERING
    BehandlingssammendragType.MELDEKORTBEHANDLING -> BehandlingssammendragTypeDTO.MELDEKORTBEHANDLING
}
