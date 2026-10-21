package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter.KanIkkeOppdatereRammebehandlingForBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter.OppdaterRammebehandlingForBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.SkalJournalføreNotatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterRammebehandlingForBeslutterService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/beslutter/oppdater"

private data class OppdaterRammebehandlingForBeslutterBody(val skalJournalføreNotat: SkalJournalføreNotatDTO)

/**
 * Beslutters endepunkt for å oppdatere rammebehandlingen mens den er under beslutning.
 */
fun Route.oppdaterRammebehandlingForBeslutterRoute(
    oppdaterRammebehandlingForBeslutterService: OppdaterRammebehandlingForBeslutterService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - beslutter oppdaterer rammebehandlingen" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                call.withBody<OppdaterRammebehandlingForBeslutterBody> { body ->
                    val correlationId = call.correlationId()
                    krevBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    oppdaterRammebehandlingForBeslutterService.oppdaterForBeslutter(
                        OppdaterRammebehandlingForBeslutterKommando(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            beslutter = saksbehandler,
                            skalJournalføreNotat = body.skalJournalføreNotat.toDomain(),
                            correlationId = correlationId,
                        ),
                    ).fold(
                        {
                            call.respondJson(statusAndValue = it.toStatusAndErrorJson())
                        },
                        { (sak) ->
                            auditService.logMedRammebehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Beslutter oppdaterer rammebehandlingen",
                                correlationId = correlationId,
                            )
                            call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId, saksbehandler))
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeOppdatereRammebehandlingForBeslutter.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeOppdatereRammebehandlingForBeslutter.RammebehandlingenErIkkeUnderBeslutning -> HttpStatusCode.Conflict to ErrorJson(
        "Behandlingen er ikke under beslutning. Status: $status.",
        "behandlingen_er_ikke_under_beslutning",
    )

    KanIkkeOppdatereRammebehandlingForBeslutter.BehandlingenErIkkeLengerUnderBeslutning -> HttpStatusCode.Conflict to ErrorJson(
        "Behandlingen er ikke lenger under beslutning. Saksbehandler kan ha angret sendingen til beslutning.",
        "behandlingen_er_ikke_under_beslutning",
    )

    KanIkkeOppdatereRammebehandlingForBeslutter.BeslutterMåVæreTildeltRammebehandlingen -> HttpStatusCode.BadRequest to ErrorJson(
        "Du kan ikke oppdatere behandlingen, da du ikke er beslutter for den.",
        "må_være_beslutter_for_behandlingen",
    )

    KanIkkeOppdatereRammebehandlingForBeslutter.RammebehandlingenErAlleredeGodkjent -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er allerede godkjent.",
        "behandlingen_er_allerede_godkjent",
    )

    KanIkkeOppdatereRammebehandlingForBeslutter.RammebehandlingenErSattPåVent -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke oppdatere behandlingen fordi den er satt på vent.",
        "behandlingen_kan_ikke_være_satt_på_vent",
    )

    KanIkkeOppdatereRammebehandlingForBeslutter.MåHaBegrunnelseForÅJournalføreNotat -> HttpStatusCode.BadRequest to Standardfeil.måHaBegrunnelseForÅJournalføreNotat()

    KanIkkeOppdatereRammebehandlingForBeslutter.MåHaBegrunnelseForBarnetilleggForÅJournalføreNotat -> HttpStatusCode.BadRequest to Standardfeil.måHaBegrunnelseForBarnetilleggForÅJournalføreNotat()
}
