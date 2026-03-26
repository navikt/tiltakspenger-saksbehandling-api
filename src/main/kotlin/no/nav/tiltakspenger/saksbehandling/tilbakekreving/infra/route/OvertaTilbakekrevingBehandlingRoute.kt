package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withTilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.service.TilbakekrevingBehandlingTildelingService
import java.time.Clock

private const val OVERTA_TILBAKEKREVING_PATH = "/sak/{sakId}/tilbakekreving/{tilbakekrevingId}/overta"

fun Route.overtaTilbakekrevingBehandlingRoute(
    auditService: AuditService,
    tilbakekrevingBehandlingTildelingService: TilbakekrevingBehandlingTildelingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    patch(OVERTA_TILBAKEKREVING_PATH) {
        logger.debug { "Mottatt patch-request på '$OVERTA_TILBAKEKREVING_PATH' - Overtar tilbakekrevingsbehandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withTilbakekrevingId { tilbakekrevingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                tilbakekrevingBehandlingTildelingService.overtaBehandling(sakId, tilbakekrevingId, saksbehandler)
                    .also { (sak) ->
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler overtar tilbakekrevingsbehandlingen",
                            correlationId = correlationId,
                            behandlingId = tilbakekrevingId,
                        )

                        call.respondJson(value = sak.toSakDTO(clock))
                    }
            }
        }
    }
}
