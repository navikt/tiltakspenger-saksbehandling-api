package no.nav.tiltakspenger.vedtak.routes.meldekort

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond409Conflict
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
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
                                is KanIkkeOppretteMeldekortBehandling.IkkeTilgangTilSak -> call.respond403Forbidden(
                                    melding = "Du har ikke tilgang til sak $sakId",
                                    kode = "",
                                )

                                is KanIkkeOppretteMeldekortBehandling.BehandlingFinnes -> call.respond409Conflict(
                                    melding = "Behandling finnes allerede for hendelseId $hendelseId på sak $sakId",
                                    kode = "",
                                )

                                is KanIkkeOppretteMeldekortBehandling.IngenMeldeperiode -> call.respond400BadRequest(
                                    melding = "Fant ikke meldeperioden med hendelseId $hendelseId på sak $sakId",
                                    kode = "",
                                )

                                is KanIkkeOppretteMeldekortBehandling.HenteNavkontorFeilet -> call.respond500InternalServerError(
                                    melding = "Kunne ikke hente Nav-kontor for brukeren",
                                    kode = "",
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

                            call.respond(status = HttpStatusCode.OK, message = "{}")
                        },
                    )
                }
            }
        }
    }
}
