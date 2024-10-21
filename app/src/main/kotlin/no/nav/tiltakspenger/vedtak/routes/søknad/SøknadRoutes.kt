package no.nav.tiltakspenger.vedtak.routes.søknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.soknad.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.vedtak.auth2.TokenService
import no.nav.tiltakspenger.vedtak.routes.withSystembruker

private val logger = KotlinLogging.logger {}

const val SØKNAD_PATH = "/soknad"

fun Route.søknadRoutes(søknadService: SøknadService, tokenService: TokenService) {
    post(SØKNAD_PATH) {
        logger.debug { "Mottatt ny søknad på '$SØKNAD_PATH' -  Prøver deserialisere og lagre." }
        call.withSystembruker(tokenService = tokenService) { systembruker ->
            val søknadDTO = call.receive<SøknadDTO>()
            logger.debug { "Deserialisert søknad OK med id ${søknadDTO.søknadId}" }
            // Oppretter sak med søknad og lagrer den
            søknadService.nySøknad(
                søknad = SøknadDTOMapper.mapSøknad(
                    dto = søknadDTO,
                    innhentet = søknadDTO.opprettet,
                ),
                systembruker = systembruker,
            )
            call.respond(message = "OK", status = HttpStatusCode.OK)
        }
    }
}
