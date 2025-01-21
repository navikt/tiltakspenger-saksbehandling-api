package no.nav.tiltakspenger.vedtak.routes.meldekort

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.meldekort.service.KanIkkeOppretteMeldekortBehandling
import no.nav.tiltakspenger.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.withMeldeperiodeHendelseId
import no.nav.tiltakspenger.vedtak.routes.withSakId

private const val PATH = "sak/{sakId}/meldeperiode/{hendelseId}/opprettBehandling"

fun Route.opprettMeldekortBehandlingRoute(
    opprettMeldekortBehandlingService: OpprettMeldekortBehandlingService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - oppretter meldekort-behandling" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldeperiodeHendelseId { hendelseId ->
                    val correlationId = call.correlationId()

                    opprettMeldekortBehandlingService.opprettBehandling(
                        hendelseId = hendelseId,
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ).fold(
                        {
                            when (it) {
                                is KanIkkeOppretteMeldekortBehandling.IkkeTilgangTilSak -> call.respond(HttpStatusCode.Forbidden)
                                is KanIkkeOppretteMeldekortBehandling.BehandlingFinnes -> call.respond(HttpStatusCode.Conflict)
                                is KanIkkeOppretteMeldekortBehandling.IngenMeldeperiode -> call.respond(HttpStatusCode.BadRequest)
                                is KanIkkeOppretteMeldekortBehandling.HenteNavkontorFeilet -> call.respond(
                                    status = HttpStatusCode.InternalServerError,
                                    message = "Kunne ikke hente Nav-kontor for utbetaling",
                                )
                            }
                        },
                        {
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.CREATE,
                                contextMessage = "Oppretter meldekort-behandling",
                                correlationId = correlationId,
                            )

                            call.respond(status = HttpStatusCode.OK, message = "Ok!")
                        },
                    )
                }
            }
        }
    }
}
