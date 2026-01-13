package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragBenktype
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSortering
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.domene.ÅpneBehandlingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkOversiktService
import no.nav.tiltakspenger.saksbehandling.benk.service.TilgangsfiltrertBenkOversikt
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody

private const val PATH = "/behandlinger"

fun Route.hentBenkRoute(
    benkOversiktService: BenkOversiktService,
) {
    val logger = KotlinLogging.logger {}

    data class HentBenkOversiktBody(
        val benktype: List<String>?,
        val behandlingstype: List<String>?,
        val status: List<String>? = null,
        val identer: List<String>? = null,
        val sortering: String,
    ) {
        val benkSortering = BenkSortering.fromString(sortering)

        fun toCommand(saksbehandler: Saksbehandler, correlationId: CorrelationId): HentÅpneBehandlingerCommand =
            HentÅpneBehandlingerCommand(
                åpneBehandlingerFiltrering = ÅpneBehandlingerFiltrering(
                    benktype = benktype?.let { benktyper -> benktyper.map { BehandlingssammendragBenktype.valueOf(it) } },
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
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        call.withBody<HentBenkOversiktBody> { body ->
            benkOversiktService.hentBenkOversikt(
                command = body.toCommand(saksbehandler, call.correlationId()),
                saksbehandlerToken = token,
                saksbehandler = saksbehandler,
            ).also {
                call.respondJson(value = it.toDTO())
            }
        }
    }
}

private fun TilgangsfiltrertBenkOversikt.toDTO(): TilgangsfiltrertBenkOversiktDTO = TilgangsfiltrertBenkOversiktDTO(
    behandlingssammendrag = this.behandlingssammendrag.toDTO(),
    totalAntall = this.totalAntall,
    antallFiltrertPgaTilgang = this.antallFiltrertPgaTilgang,
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
    erSattPåVent = erSattPåVent,
)

private fun BehandlingssammendragStatus.toBehandlingssammendragStatusDto(): BehandlingssammendragStatusDto =
    when (this) {
        BehandlingssammendragStatus.UNDER_AUTOMATISK_BEHANDLING -> BehandlingssammendragStatusDto.UNDER_AUTOMATISK_BEHANDLING
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
    BehandlingssammendragType.KORRIGERT_MELDEKORT -> BehandlingssammendragTypeDTO.KORRIGERT_MELDEKORT
    BehandlingssammendragType.KLAGEBEHANDLING -> BehandlingssammendragTypeDTO.KLAGEBEHANDLING
}
