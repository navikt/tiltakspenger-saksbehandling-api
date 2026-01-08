package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.texas.systembruker
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.getSystemBrukerMapper
import no.nav.tiltakspenger.saksbehandling.felles.krevHentEllerOpprettSakRollen
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.person.infra.route.FnrDTO

const val SAKSNUMMER_PATH = "/saksnummer"

fun Route.hentEllerOpprettSakSystembrukerRoute(
    sakService: SakService,
) {
    val logger = KotlinLogging.logger {}

    post(SAKSNUMMER_PATH) {
        logger.debug { "Mottatt kall på '$SAKSNUMMER_PATH' - henter eller oppretter sak." }
        val systembruker = call.systembruker(getSystemBrukerMapper()) as? Systembruker ?: return@post
        krevHentEllerOpprettSakRollen(systembruker)
        call.withBody<FnrDTO> { body ->
            val fnr: Fnr = body.toDomain()
            val (sak, _) = sakService.hentEllerOpprettSak(
                fnr = fnr,
                correlationId = CorrelationId.generate(),
            )
            call.respondJson(value = SaksnummerResponse(saksnummer = sak.saksnummer.verdi))
        }
    }
}

data class SaksnummerResponse(
    val saksnummer: String,
)
