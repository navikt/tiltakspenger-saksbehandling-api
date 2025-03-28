package no.nav.tiltakspenger.saksbehandling.routes.søknad

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.soknad.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService

private val logger = KotlinLogging.logger {}

const val SØKNAD_PATH = "/soknad"

fun Route.søknadRoutes(
    søknadService: SøknadService,
    sakService: SakService,
    tokenService: TokenService,
) {
    post(SØKNAD_PATH) {
        logger.debug { "Mottatt ny søknad på '$SØKNAD_PATH' -  Prøver deserialisere og lagre." }
        call.withSystembruker(tokenService = tokenService) { systembruker: Systembruker ->
            val søknadDTO = call.receive<SøknadDTO>()
            logger.debug { "Deserialisert søknad OK med id ${søknadDTO.søknadId}" }
            val sak = sakService.hentForSaksnummer(Saksnummer(søknadDTO.saksnummer), systembruker)
            // Oppretter søknad og lagrer den med kobling til angitt sak
            søknadService.nySøknad(
                søknad = SøknadDTOMapper.mapSøknad(
                    dto = søknadDTO,
                    innhentet = søknadDTO.opprettet,
                    sak = sak,
                ),
                systembruker = systembruker,
            )
            call.respond(message = "OK", status = HttpStatusCode.OK)
        }
    }
}
