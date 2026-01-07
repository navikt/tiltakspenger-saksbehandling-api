package no.nav.tiltakspenger.saksbehandling.infra.setup

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
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.ikkeFunnet
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.serverfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.ugyldigJournalpostIdInput
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.ugyldigRequest
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.JournalpostIdInputValidationException

object ExceptionHandler {
    private val logger = KotlinLogging.logger {}
    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ) {
        val uri = call.request.uri
        val loggmelding = "Feil mot frontend: ${cause.message}. Uri: $uri"
        when (cause) {
            is IllegalStateException -> {
                logger.error(cause) { loggmelding }
                call.respond500InternalServerError(serverfeil())
            }

            is ContentTransformationException -> {
                logger.error(cause) { loggmelding }
                call.run { respond400BadRequest(errorJson = ugyldigRequest()) }
            }

            is JournalpostIdInputValidationException -> {
                logger.warn(cause) { loggmelding }
                call.run {
                    respond400BadRequest(
                        errorJson = ugyldigJournalpostIdInput(
                            melding = cause.message ?: "Ugyldig journalpostId",
                        ),
                    )
                }
            }

            is TilgangException -> {
                logger.warn(cause) { loggmelding }
                call.respond403Forbidden(cause.toErrorJson())
            }

            is IkkeFunnetException -> {
                logger.error(cause) { loggmelding }
                call.respond404NotFound(ikkeFunnet())
            }

            // Catch all
            else -> {
                logger.error(cause) { loggmelding }
                call.respond500InternalServerError(serverfeil())
            }
        }
    }
}
