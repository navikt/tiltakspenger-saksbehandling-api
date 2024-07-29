package no.nav.tiltakspenger.vedtak.routes.rivers.søknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.service.SøknadService

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")
const val søknadpath = "/rivers/soknad"

fun Route.søknadRoutes(
    søknadService: SøknadService,
) {
    post(søknadpath) {
        LOG.info { "Vi har mottatt søknad fra river" }
        try {
            val søknadDTO = call.receive<SøknadDTO>()

            // Oppretter sak med søknad og lagrer den
            søknadService.nySøknad(
                søknad = SøknadDTOMapper.mapSøknad(
                    dto = søknadDTO,
                    innhentet = søknadDTO.opprettet,
                ),
            )
        } catch (exception: Exception) {
            SECURELOG.error { "Feil ved mottak av søknad fra rivers. $exception" }
        }

        call.respond(message = "OK", status = HttpStatusCode.OK)
    }
}
