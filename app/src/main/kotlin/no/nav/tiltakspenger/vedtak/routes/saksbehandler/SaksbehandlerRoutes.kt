package no.nav.tiltakspenger.vedtak.routes.saksbehandler

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.vedtak.auth2.TokenService
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.withSaksbehandler

internal const val SAKSBEHANDLER_PATH = "/saksbehandler"

internal fun Route.saksbehandlerRoutes(
    tokenService: TokenService,
) {
    get(SAKSBEHANDLER_PATH) {
        call.withSaksbehandler(tokenService = tokenService) { saksbehandler ->
            call.respond(message = saksbehandler.toDTO(), status = HttpStatusCode.OK)
        }
    }
}
