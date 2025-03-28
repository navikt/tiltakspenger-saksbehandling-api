package no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.uri
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeFunnet
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.serverfeil
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ugyldigRequest

object ExceptionHandler {
    private val logger = KotlinLogging.logger {}
    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ) {
        val uri = call.request.uri
        logger.error(cause) { "Ktor mottok exception i ytterste lag. Uri: $uri." }
        when (cause) {
            is IllegalStateException -> {
                call.respond500InternalServerError(serverfeil())
            }

            is ContentTransformationException -> {
                call.run { respond400BadRequest(errorJson = ugyldigRequest()) }
            }

            is TilgangException -> {
                call.respond403Forbidden(ikkeTilgang())
            }

            is IkkeFunnetException -> {
                call.respond404NotFound(ikkeFunnet())
            }

            // Catch all
            else -> {
                call.respond500InternalServerError(serverfeil())
            }
        }
    }
}
