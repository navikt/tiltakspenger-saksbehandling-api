package no.nav.tiltakspenger.saksbehandling.routes.meldekort

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortKorrigering
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortKorrigeringService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.routes.withSakId

private const val PATH = "sak/{sakId}/meldeperiode/{meldekortId}/opprettKorrigering"

fun Route.opprettMeldekortKorrigeringRoute(
    opprettMeldekortKorrigeringService: OpprettMeldekortKorrigeringService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - oppretter korrigering av meldekort-behandling" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    val correlationId = call.correlationId()

                    opprettMeldekortKorrigeringService.opprettKorrigering(
                        meldekortId = meldekortId,
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ).fold(
                        {
                            when (it) {
                                is KanIkkeOppretteMeldekortKorrigering.IkkeTilgangTilSak -> call.respond403Forbidden(
                                    melding = "Du har ikke tilgang til sak $sakId",
                                    kode = "",
                                )

                                is KanIkkeOppretteMeldekortKorrigering.BehandlingenFinnesIkke -> call.respond400BadRequest(
                                    melding = "Fant ikke meldekortbehandlingen $meldekortId på sak $sakId",
                                    kode = "",
                                )

                                is KanIkkeOppretteMeldekortKorrigering.BehandlingenIkkeGodkjent -> call.respond400BadRequest(
                                    melding = "Meldekortbehandlingen $meldekortId på sak $sakId er ikke godkjent",
                                    kode = "",
                                )
                            }
                        },
                        {
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.CREATE,
                                contextMessage = "Oppretter korrigering av meldekort",
                                correlationId = correlationId,
                            )

                            call.respond(HttpStatusCode.OK, it.toDTO())
                        },
                    )
                }
            }
        }
    }
}
