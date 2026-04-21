package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId

// TODO abn: Denne er ikke i bruk av sbh frontend og kan antagelig fjernes

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}"

fun Route.hentRammebehandlingRoute(
    behandlingService: RammebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    get(PATH) {
        logger.debug { "Mottatt get-request på '$PATH' - henter rammebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@get
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                behandlingService.hentSakOgRammebehandling(sakId, behandlingId).also { (sak) ->
                    auditService.logMedRammebehandlingId(
                        behandlingId = behandlingId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.ACCESS,
                        contextMessage = "Henter rammebehandling",
                        correlationId = correlationId,
                    )
                    call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                }
            }
        }
    }
}
