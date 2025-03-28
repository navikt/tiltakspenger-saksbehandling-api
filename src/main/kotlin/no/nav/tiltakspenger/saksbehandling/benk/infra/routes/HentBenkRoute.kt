package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.BEHANDLINGER_PATH
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeHenteSaksoversikt
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSaksoversiktDTO

fun Route.hentBenkRoute(
    tokenService: TokenService,
    sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
) {
    val logger = KotlinLogging.logger {}

    get(BEHANDLINGER_PATH) {
        logger.debug { "Mottatt get-request på $BEHANDLINGER_PATH for å hente alle behandlinger på benken" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            sakService.hentBenkOversikt(
                saksbehandler = saksbehandler,
                correlationId = call.correlationId(),
            ).fold(
                {
                    when (it) {
                        is no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeHenteSaksoversikt.HarIkkeTilgang -> call.respond403Forbidden(
                            ikkeTilgang("Må ha en av rollene ${it.kreverEnAvRollene} for å hente behandlinger på benken."),
                        )
                    }
                },
                {
                    call.respond(status = HttpStatusCode.OK, it.toSaksoversiktDTO())
                },
            )
        }
    }
}
