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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.GjenopptaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.KunneIkkeGjenopptaBehandling
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId

private const val GJENNOPPTA_BEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/gjenoppta"

fun Route.gjenopptaRammebehandling(
    auditService: AuditService,
    gjenopptaBehandlingService: GjenopptaRammebehandlingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(GJENNOPPTA_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$GJENNOPPTA_BEHANDLING_PATH' - Gjenopptar rammebehandling." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                gjenopptaBehandlingService.gjenopptaBehandling(
                    GjenopptaRammebehandlingKommando(
                        sakId = sakId,
                        rammebehandlingId = behandlingId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ),
                ).fold(
                    ifLeft = {
                        call.respondJson(statusAndValue = it.tilStatusOgErrorJson())
                    },
                    ifRight = { (sak) ->
                        auditService.logMedRammebehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Gjenopptar behandling",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}

private fun KunneIkkeGjenopptaBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KunneIkkeGjenopptaBehandling.FeilVedOppdateringAvSaksopplysninger -> this.originalFeil.tilStatusOgErrorJson()
}
