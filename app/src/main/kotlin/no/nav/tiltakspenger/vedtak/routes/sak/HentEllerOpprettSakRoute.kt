package no.nav.tiltakspenger.vedtak.routes.sak

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.Systembruker
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

const val SAKSNUMMER_PATH = "/saksnummer"

fun Route.hentEllerOpprettSakRoute(
    sakService: SakService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}

    post(SAKSNUMMER_PATH) {
        logger.debug { "Mottatt kall på '$SAKSNUMMER_PATH' - henter eller oppretter sak." }
        call.withSystembruker(tokenService = tokenService) { systembruker: Systembruker ->
            val fnr = call.receive<FnrDTO>().fnr
            val sak = sakService.hentEllerOpprettSak(
                fnr = Fnr.fromString(fnr),
                systembruker = systembruker,
                correlationId = CorrelationId.generate(),
            )
            call.respond(message = SaksnummerResponse(saksnummer = sak.saksnummer.verdi), status = HttpStatusCode.OK)
        }
    }
}

data class SaksnummerResponse(
    val saksnummer: String,
)
