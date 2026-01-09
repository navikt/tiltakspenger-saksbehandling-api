package no.nav.tiltakspenger.saksbehandling.saksbehandler.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.toSaksbehandlerDTO

internal const val SAKSBEHANDLER_PATH = "/saksbehandler"

private val logger = KotlinLogging.logger { }

/** Henter informasjon om saksbehandleren som er logget inn.*/
internal fun Route.meRoute() {
    get(SAKSBEHANDLER_PATH) {
        logger.debug { "Mottatt get-request på $SAKSBEHANDLER_PATH" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.respondJson(value = saksbehandler.toSaksbehandlerDTO())
    }
}
