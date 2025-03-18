package no.nav.tiltakspenger.saksbehandling.routes.meldekort

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortKorrigering
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortKorrigeringService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.routes.withSakId

private const val PATH = "sak/{sakId}/meldeperiode/{meldeperiodeKjedeId}/opprettKorrigering"

fun Route.opprettMeldekortKorrigeringRoute(
    opprettMeldekortKorrigeringService: OpprettMeldekortKorrigeringService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - oppretter korrigering av meldekort" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldeperiodeKjedeId { kjedeId ->
                    val correlationId = call.correlationId()

                    opprettMeldekortKorrigeringService.opprettKorrigering(
                        kjedeId = kjedeId,
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
                                is KanIkkeOppretteMeldekortKorrigering.HenteNavkontorFeilet -> call.respond500InternalServerError(
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
