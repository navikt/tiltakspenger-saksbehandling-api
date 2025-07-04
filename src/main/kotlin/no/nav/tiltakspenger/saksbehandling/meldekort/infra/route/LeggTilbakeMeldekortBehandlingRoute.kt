package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.LeggTilbakeMeldekortBehandlingService

private const val LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/legg-tilbake"

fun Route.leggTilbakeMeldekortBehandlingRoute(
    tokenService: TokenService,
    auditService: AuditService,
    leggTilbakeMeldekortBehandlingService: LeggTilbakeMeldekortBehandlingService,
) {
    val logger = KotlinLogging.logger {}
    post(LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH' - Fjerner saksbehandler/beslutter fra meldekortbehandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()

                leggTilbakeMeldekortBehandlingService.leggTilbakeMeldekortBehandling(
                    meldekortId,
                    saksbehandler,
                    correlationId = correlationId,
                ).also {
                    auditService.logMedMeldekortId(
                        meldekortId = meldekortId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = "Saksbehandler fjernes fra meldekortbehandlingen",
                        correlationId = correlationId,
                    )

                    call.respond(
                        status = HttpStatusCode.OK,
                        it.tilMeldekortBehandlingDTO(),
                    )
                }
            }
        }
    }
}
