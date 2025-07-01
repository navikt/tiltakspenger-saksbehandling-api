package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

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
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import java.time.Clock

fun Route.hentSakForFnrRoute(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
    clock: Clock,
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

                sakService.hentForFnr(fnr, saksbehandler, correlationId).also {
                    auditService.logMedBrukerId(
                        brukerId = fnr,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.SEARCH,
                        contextMessage = "Søker opp alle sakene til brukeren",
                        correlationId = correlationId,
                    )
                    if (it == null) {
                        call.respond404NotFound(Standardfeil.fantIkkeFnr())
                    } else {
                        val sakDTO = it.toSakDTO(clock)
                        call.respond(message = sakDTO, status = HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
