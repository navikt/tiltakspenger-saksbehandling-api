package no.nav.tiltakspenger.saksbehandling.routes.saksbehandler

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.dto.toDTO

internal const val SAKSBEHANDLER_PATH = "/saksbehandler"

private val logger = KotlinLogging.logger { }

internal fun Route.saksbehandlerRoutes(
    tokenService: TokenService,
) {
    get(SAKSBEHANDLER_PATH) {
        logger.debug { "Mottatt get-request på $SAKSBEHANDLER_PATH" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.respond(message = saksbehandler.toDTO(), status = HttpStatusCode.OK)
        }
    }
}
