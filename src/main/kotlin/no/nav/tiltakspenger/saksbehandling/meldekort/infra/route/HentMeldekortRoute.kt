package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
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

                    // Sjekker om saksbehandler har tilgang til person og har en rolle SAKSBEHANDLER eller BESLUTTER
                    val sak = sakService.sjekkTilgangOgHentForSakId(sakId, saksbehandler, correlationId = correlationId)

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
