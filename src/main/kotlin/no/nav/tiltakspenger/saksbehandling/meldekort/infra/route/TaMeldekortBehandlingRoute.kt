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
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldekortBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.TaMeldekortBehandlingService

private const val TA_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/ta"

fun Route.taMeldekortBehandlingRoute(
    tokenService: TokenService,
    auditService: AuditService,
    taMeldekortBehandlingService: TaMeldekortBehandlingService,
) {
    val logger = KotlinLogging.logger {}
    post(TA_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$TA_MELDEKORTBEHANDLING_PATH' - Knytter beslutter til meldekortbehandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()

                taMeldekortBehandlingService.taMeldekortBehandling(
                    meldekortId,
                    saksbehandler,
                    correlationId = correlationId,
                ).also {
                    auditService.logMedMeldekortId(
                        meldekortId = meldekortId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = "Beslutter tar meldekortbehandlingen",
                        correlationId = correlationId,
                    )

                    call.respond(
                        status = HttpStatusCode.OK,
                        it.toMeldekortBehandlingDTO(UtbetalingsstatusDTO.IKKE_GODKJENT),
                    )
                }
            }
        }
    }
}
