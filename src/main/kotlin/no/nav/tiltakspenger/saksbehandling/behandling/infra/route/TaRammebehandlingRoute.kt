package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta.toStatusAndErrorJson

private const val TA_BEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/ta"

fun Route.taRammebehandlingRoute(
    auditService: AuditService,
    taBehandlingService: TaRammebehandlingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(TA_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$TA_BEHANDLING_PATH' - Knytter saksbehandler/beslutter til behandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                taBehandlingService.taBehandling(sakId, behandlingId, saksbehandler).fold(
                    ifLeft = {
                        call.respondJson(statusAndValue = it.tilStatusOgErrorJson())
                    },
                    ifRight = {
                        auditService.logMedRammebehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler tar behandlingen",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = it.first.tilRammebehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}

fun KunneIkkeTaBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KunneIkkeTaBehandling.BehandlingenErIEnTilstandSomIkkeTillaterÅTaBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er i en tilstand som ikke tillater å ta behandlingen.",
        "behandlingen_er_i_en_tilstand_som_ikke_tillater_å_ta_behandling",
    )

    KunneIkkeTaBehandling.BehandlingenHarEksisterendeBeslutter -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen har allerede en beslutter.",
        "behandlingen_har_allerede_en_beslutter",
    )

    KunneIkkeTaBehandling.BehandlingenHarEksisterendeSaksbehandler -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen har allerede en saksbehandler.",
        "behandlingen_har_allerede_en_saksbehandler",
    )

    is KunneIkkeTaBehandling.FeilVedKlagebehandling -> this.originalfeil.toStatusAndErrorJson()

    KunneIkkeTaBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSammePåBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Saksbehandler og beslutter kan ikke være den samme på behandlingen.",
        "saksbehandler_og_beslutter_kan_ikke_være_den_samme_på_behandlingen",
    )
}
