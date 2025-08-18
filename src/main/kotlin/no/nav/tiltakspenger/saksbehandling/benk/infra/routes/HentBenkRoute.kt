package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragBenktype
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSortering
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.domene.ÅpneBehandlingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkOversiktService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody

private const val PATH = "/behandlinger"

fun Route.hentBenkRoute(
    tokenService: TokenService,
    benkOversiktService: BenkOversiktService,
) {
    val logger = KotlinLogging.logger {}

    data class HentBenkOversiktBody(
        val benktype: String?,
        val behandlingstype: List<String>?,
        val status: List<String>? = null,
        val identer: List<String>? = null,
        val sortering: String,
    ) {
        val benkSortering = BenkSortering.fromString(sortering)

        fun toCommand(saksbehandler: Saksbehandler, correlationId: CorrelationId): HentÅpneBehandlingerCommand =
            HentÅpneBehandlingerCommand(
                åpneBehandlingerFiltrering = ÅpneBehandlingerFiltrering(
                    benktype = benktype?.let { BehandlingssammendragBenktype.valueOf(it) } ?: BehandlingssammendragBenktype.KLAR,
                    behandlingstype = behandlingstype?.map { BehandlingssammendragType.valueOf(it) },
                    status = status?.map { BehandlingssammendragStatus.valueOf(it) },
                    identer = identer,
                ),
                sortering = benkSortering,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
            )
    }

    post(PATH) {
        logger.debug { "Mottatt get-request på $PATH for å hente alle behandlinger på benken" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBody<HentBenkOversiktBody> {
                benkOversiktService.hentBenkOversikt(
                    command = it.toCommand(saksbehandler, call.correlationId()),
                ).also {
                    call.respond(status = HttpStatusCode.OK, it.toDTO())
                }
            }
        }
    }
}

private fun BenkOversikt.toDTO(): BenkOversiktDTO = BenkOversiktDTO(
    behandlingssammendrag = this.behandlingssammendrag.toDTO(),
    totalAntall = this.totalAntall,
)

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
    sistEndret = sistEndret?.toString(),
)

private fun BehandlingssammendragStatus.toBehandlingssammendragStatusDto(): BehandlingssammendragStatusDto =
    when (this) {
        BehandlingssammendragStatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatusDto.KLAR_TIL_BEHANDLING
        BehandlingssammendragStatus.UNDER_BEHANDLING -> BehandlingssammendragStatusDto.UNDER_BEHANDLING
        BehandlingssammendragStatus.KLAR_TIL_BESLUTNING -> BehandlingssammendragStatusDto.KLAR_TIL_BESLUTNING
        BehandlingssammendragStatus.UNDER_BESLUTNING -> BehandlingssammendragStatusDto.UNDER_BESLUTNING
    }

private fun BehandlingssammendragType.toDTO(): BehandlingssammendragTypeDTO = when (this) {
    BehandlingssammendragType.SØKNADSBEHANDLING -> BehandlingssammendragTypeDTO.SØKNADSBEHANDLING
    BehandlingssammendragType.REVURDERING -> BehandlingssammendragTypeDTO.REVURDERING
    BehandlingssammendragType.MELDEKORTBEHANDLING -> BehandlingssammendragTypeDTO.MELDEKORTBEHANDLING
    BehandlingssammendragType.INNSENDT_MELDEKORT -> BehandlingssammendragTypeDTO.INNSENDT_MELDEKORT
}
