package no.nav.tiltakspenger.saksbehandling.routes.sak

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.UgyldigFnrException
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.routes.withBody
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KunneIkkeHenteSakForFnr
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService

fun Route.hentSakForFnrRoute(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger {}

    post(SAK_PATH) {
        logger.debug { "Mottatt post-request på $SAK_PATH" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBody<FnrDTO> { body ->
                val fnr = Either.catch { Fnr.fromString(body.fnr) }.getOrElse {
                    when (it) {
                        is UgyldigFnrException -> call.respond400BadRequest(
                            melding = "Forventer at fødselsnummeret er 11 siffer",
                            kode = "ugyldig_fnr",
                        )

                        else -> call.respond500InternalServerError(
                            melding = "Ukjent feil ved lesing av fødselsnummeret",
                            kode = "fnr_parsing_feil",
                        )
                    }
                    return@withBody
                }
                val correlationId = call.correlationId()

                sakService.hentForFnr(fnr, saksbehandler, correlationId).fold(
                    ifLeft = {
                        when (it) {
                            is KunneIkkeHenteSakForFnr.FantIkkeSakForFnr -> call.respond404NotFound(Standardfeil.fantIkkeFnr())
                            is KunneIkkeHenteSakForFnr.HarIkkeTilgang -> call.respond403Forbidden(ikkeTilgang("Må ha en av rollene ${it.kreverEnAvRollene} for å hente sak for fnr."))
                        }
                    },
                    ifRight = {
                        auditService.logMedBrukerId(
                            brukerId = fnr,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.SEARCH,
                            contextMessage = "Søker opp alle sakene til brukeren",
                            correlationId = correlationId,
                        )
                        val sakDTO = it.toDTO()
                        call.respond(message = sakDTO, status = HttpStatusCode.OK)
                    },
                )
            }
        }
    }
}
