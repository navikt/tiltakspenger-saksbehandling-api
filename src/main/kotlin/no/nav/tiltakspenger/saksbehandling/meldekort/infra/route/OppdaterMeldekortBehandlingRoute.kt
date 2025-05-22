package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.MeldekortperiodenKanIkkeVæreFremITid
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortService
import java.time.Clock

private const val PATH = "/sak/{sakId}/meldekort/{meldekortId}/oppdater"

fun Route.oppdaterMeldekortBehandlingRoute(
    oppdaterMeldekortService: OppdaterMeldekortService,
    auditService: AuditService,
    tokenService: TokenService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - saksbehandler har oppdatert et meldekort under behandling" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    call.withBody<OppdaterMeldekortDTO> { body ->
                        val correlationId = call.correlationId()
                        val kommando = body.toDomain(
                            saksbehandler = saksbehandler,
                            meldekortId = meldekortId,
                            sakId = sakId,
                            correlationId = correlationId,
                        )
                        oppdaterMeldekortService.oppdaterMeldekort(kommando).fold(
                            ifLeft = {
                                respondWithError(it)
                            },
                            ifRight = {
                                auditService.logMedMeldekortId(
                                    meldekortId = meldekortId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Saksbehandler har oppdatert et meldekort under behandling",
                                    correlationId = correlationId,
                                )
                                call.respond(message = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock), status = HttpStatusCode.OK)
                            },
                        )
                    }
                }
            }
        }
    }
}

suspend fun RoutingContext.respondWithError(meldekort: KanIkkeOppdatereMeldekort) {
    when (meldekort) {
        is MeldekortperiodenKanIkkeVæreFremITid -> {
            call.respond400BadRequest(
                melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
            )
        }
    }
}
