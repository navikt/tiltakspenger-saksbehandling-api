package no.nav.tiltakspenger.saksbehandling.routes.meldekort

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.routes.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import java.time.Clock

private const val PATH = "/sak/{sakId}/meldeperiode/{kjedeId}"

fun Route.hentMeldekortRoute(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    get(PATH) {
        logger.debug { "Mottatt get-request på $PATH" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldeperiodeKjedeId { kjedeId ->
                    val correlationId = call.correlationId()

                    val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId = correlationId).getOrElse {
                        when (it) {
                            is KunneIkkeHenteSakForSakId.HarIkkeTilgang -> call.respond403Forbidden(
                                ikkeTilgang(
                                    "Må ha en av rollene ${it.kreverEnAvRollene} for å hente meldekort",
                                ),
                            )
                        }
                        return@withMeldeperiodeKjedeId
                    }

                    val meldeperiodeKjedeDTO = sak.toMeldeperiodeKjedeDTO(kjedeId = kjedeId, clock = clock)

                    auditService.logMedSakId(
                        sakId = sakId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.ACCESS,
                        contextMessage = "Henter meldekort",
                        correlationId = correlationId,
                    )
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = meldeperiodeKjedeDTO,
                    )
                }
            }
        }
    }
}
