package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SAK_PATH
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.service.TiltaksdeltagelseService

fun Route.hentTiltakdeltagelserRoute(
    tiltaksdeltagelseService: TiltaksdeltagelseService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    get("$SAK_PATH/{sakId}/tiltaksdeltagelser") {
        logger.debug { "Mottatt get-request på '$SAK_PATH/{sakId}/tiltaksdeltagelser' - henter tiltaksdeltagelser for en sak" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            val correlationId = call.correlationId()
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
            val tiltaksdeltagelser = tiltaksdeltagelseService.hentTiltaksdeltagelserForSak(sakId, correlationId).map {
                it.toDTO()
            }
            auditService.logMedSakId(
                sakId = sakId,
                navIdent = saksbehandler.navIdent,
                action = AuditLogEvent.Action.ACCESS,
                contextMessage = "Henter tiltaksdeltagelser for en sak",
                correlationId = correlationId,
            )
            call.respond(status = HttpStatusCode.OK, tiltaksdeltagelser)
        }
    }
}
