package no.nav.tiltakspenger.saksbehandling.routes.sak

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.routes.withSaksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KunneIkkeHenteSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
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
                ).fold(
                    {
                        when (it) {
                            is KunneIkkeHenteSakForSaksnummer.HarIkkeTilgang -> call.respond403Forbidden(ikkeTilgang("Må ha en av rollene ${it.kreverEnAvRollene} for å hente sak for saksnummer."))
                        }
                    },
                    { sak ->
                        call.respond(message = sak.toDTO(clock), status = HttpStatusCode.OK)
                    },
                )
            }
        }
    }
}
