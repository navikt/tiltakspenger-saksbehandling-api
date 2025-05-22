package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSaksnummer
import java.time.Clock

fun Route.hentSakForSaksnummerRoute(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    get("$SAK_PATH/{saksnummer}") {
        logger.debug { "Mottatt get-request på $SAK_PATH/{saksnummer}" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSaksnummer { saksnummer ->
                auditService.logMedSaksnummer(
                    saksnummer = saksnummer,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.ACCESS,
                    contextMessage = "Henter hele saken til brukeren",
                    correlationId = call.correlationId(),
                )
                sakService.hentForSaksnummer(
                    saksnummer = saksnummer,
                    saksbehandler = saksbehandler,
                    correlationId = call.correlationId(),
                ).also { sak ->
                    call.respond(message = sak.toSakDTO(clock), status = HttpStatusCode.OK)
                }
            }
        }
    }
}
