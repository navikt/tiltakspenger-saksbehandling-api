package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.BEHANDLINGER_PATH
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
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
    fnr = fnr.verdi,
    saksnummer = saksnummer.verdi,
    startet = startet.toString(),
    behandlingstype = behandlingstype.toDTO(),
    status = status?.toBehandlingssammendragStatusDto(),
    saksbehandler = saksbehandler,
    beslutter = beslutter,
)

private fun Behandlingsstatus.toBehandlingssammendragStatusDto(): BehandlingssammendragStatusDto = when (this) {
    Behandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatusDto.KLAR_TIL_BEHANDLING
    Behandlingsstatus.UNDER_BEHANDLING -> BehandlingssammendragStatusDto.UNDER_BEHANDLING
    Behandlingsstatus.KLAR_TIL_BESLUTNING -> BehandlingssammendragStatusDto.KLAR_TIL_BESLUTNING
    Behandlingsstatus.UNDER_BESLUTNING -> BehandlingssammendragStatusDto.UNDER_BESLUTNING
    Behandlingsstatus.VEDTATT -> throw IllegalStateException("Vedtatt behandling er ikke støttet for å vises i benkoversikten")
    Behandlingsstatus.AVBRUTT -> throw IllegalStateException("Avbrutt behandling er ikke støttet for å vises i benkoversikten")
}

private fun BehandlingssammendragType.toDTO(): BehandlingssammendragTypeDTO = when (this) {
    BehandlingssammendragType.SØKNADSBEHANDLING -> BehandlingssammendragTypeDTO.SØKNADSBEHANDLING
    BehandlingssammendragType.REVURDERING -> BehandlingssammendragTypeDTO.REVURDERING
    BehandlingssammendragType.MELDEKORTBEHANDLING -> BehandlingssammendragTypeDTO.MELDEKORTBEHANDLING
}
