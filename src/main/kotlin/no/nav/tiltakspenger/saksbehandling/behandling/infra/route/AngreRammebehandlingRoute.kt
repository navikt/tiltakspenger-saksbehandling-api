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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.angre.KunneIkkeAngreBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.AngreRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/angre"

fun Route.angreRammebehandlingRoute(
    auditService: AuditService,
    angreBehandlingService: AngreRammebehandlingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Angrer saksbehandlers send til beslutning." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                angreBehandlingService.angreBehandling(sakId, behandlingId, saksbehandler).fold(
                    ifLeft = { feil ->
                        call.loggOgSvarFeil(
                            logger = logger,
                            operasjon = "Angre rammebehandling",
                            feil = feil,
                            statusOgErrorJson = feil.tilStatusOgErrorJson(),
                            kontekst = "sakId=$sakId, behandlingId=$behandlingId",
                        )
                    },
                    ifRight = { (sak) ->
                        auditService.logMedRammebehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler angrer behandlingen",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                    },
                )
            }
        }
    }
}

fun KunneIkkeAngreBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KunneIkkeAngreBehandling.BehandlingenErIEnTilstandSomIkkeTillaterÅAngre -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke angre behandling med status $status.",
        "behandlingen_kan_ikke_angres",
    )

    KunneIkkeAngreBehandling.MåVæreSaksbehandlerForBehandlingen -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandleren som er tildelt behandlingen for å angre.",
        "maa_vaere_saksbehandler_for_behandlingen",
    )
}
