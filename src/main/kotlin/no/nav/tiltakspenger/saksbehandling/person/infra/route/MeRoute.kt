package no.nav.tiltakspenger.saksbehandling.person.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.repo.toSaksbehandlerDTO

internal const val SAKSBEHANDLER_PATH = "/saksbehandler"

private val logger = KotlinLogging.logger { }

/** Henter informasjon om saksbehandleren som er logget inn.*/
internal fun Route.meRoute(
    tokenService: TokenService,
) {
    get(SAKSBEHANDLER_PATH) {
        logger.debug { "Mottatt get-request på $SAKSBEHANDLER_PATH" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.respond(message = saksbehandler.toSaksbehandlerDTO(), status = HttpStatusCode.OK)
        }
    }
}
