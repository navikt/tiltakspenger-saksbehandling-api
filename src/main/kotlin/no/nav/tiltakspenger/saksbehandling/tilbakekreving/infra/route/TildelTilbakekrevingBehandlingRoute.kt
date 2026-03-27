package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.route.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.withTilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.service.TilbakekrevingBehandlingTildelingService
import java.time.Clock

private const val TA_TILBAKEKREVING_PATH = "/sak/{sakId}/tilbakekreving/{tilbakekrevingId}/tildel"

fun Route.tildelTilbakekrevingBehandlingRoute(
    auditService: AuditService,
    tilbakekrevingBehandlingTildelingService: TilbakekrevingBehandlingTildelingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    post(TA_TILBAKEKREVING_PATH) {
        logger.debug { "Mottatt post-request på '$TA_TILBAKEKREVING_PATH' - Knytter saksbehandler/beslutter til tilbakekrevingsbehandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withTilbakekrevingId { tilbakekrevingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                tilbakekrevingBehandlingTildelingService.tildelBehandling(sakId, tilbakekrevingId, saksbehandler)
                    .also { (sak) ->
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler tar tilbakekrevingsbehandlingen",
                            correlationId = correlationId,
                            behandlingId = tilbakekrevingId,
                        )

                        call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                    }
            }
        }
    }
}
