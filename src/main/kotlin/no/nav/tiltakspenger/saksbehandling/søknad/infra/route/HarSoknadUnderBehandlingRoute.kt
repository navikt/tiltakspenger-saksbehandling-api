package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSystembruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevLageHendelserRollen
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.FnrDTO

const val HAR_SOKNAD_PATH = "/har-soknad"

fun Route.harSoknadUnderBehandlingRoute(
    søknadService: SøknadService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}

    post(HAR_SOKNAD_PATH) {
        logger.debug { "Mottatt kall på '$HAR_SOKNAD_PATH' - sjekker om bruker har en åpen søknad under behandling." }
        call.withSystembruker(tokenService = tokenService) { systembruker: Systembruker ->
            krevLageHendelserRollen(systembruker)

            val fnr = call.receive<FnrDTO>().fnr
            val harSoknadUnderBehandling = søknadService.harSoknadUnderBehandling(Fnr.fromString(fnr))
            call.respond(message = HarSoknadUnderBehandlingResponse(harSoknadUnderBehandling = harSoknadUnderBehandling), status = HttpStatusCode.OK)
        }
    }
}

data class HarSoknadUnderBehandlingResponse(
    val harSoknadUnderBehandling: Boolean,
)
