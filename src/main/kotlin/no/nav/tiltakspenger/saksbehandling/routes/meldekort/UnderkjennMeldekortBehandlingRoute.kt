package no.nav.tiltakspenger.saksbehandling.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KunneIkkeUnderkjenneMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto.UtbetalingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto.toMeldekortBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.withBody
import no.nav.tiltakspenger.saksbehandling.routes.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.routes.withSakId

internal const val UNDERKJENN_MELDEKORT_BEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/underkjenn"

private data class UnderkjennMeldekortBehandlingBody(val begrunnelse: String)

fun Route.underkjennMeldekortBehandlingRoute(
    underkjennMeldekortBehandlingService: UnderkjennMeldekortBehandlingService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }
    post(UNDERKJENN_MELDEKORT_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på $UNDERKJENN_MELDEKORT_BEHANDLING_PATH - Beslutter ønsker å underkjenne" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId {
                call.withMeldekortId { meldekortId ->
                    call.withBody<UnderkjennMeldekortBehandlingBody> { body ->
                        val correlationId = call.correlationId()
                        underkjennMeldekortBehandlingService.underkjenn(
                            UnderkjennMeldekortBehandlingCommand(
                                meldekortId = meldekortId,
                                begrunnelse = body.begrunnelse,
                                saksbehandler = saksbehandler,
                                correlationId = correlationId,
                            ),
                        ).fold(
                            ifLeft = {
                                val (status, message) = it.toErrorJson()
                                call.respond(status, message)
                            },
                            ifRight = {
                                auditService.logMedMeldekortId(
                                    meldekortId = meldekortId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Beslutter underkjenner meldekort $meldekortId",
                                    correlationId = correlationId,
                                )

                                call.respond(HttpStatusCode.OK, it.toMeldekortBehandlingDTO(UtbetalingsstatusDTO.IKKE_GODKJENT))
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun KunneIkkeUnderkjenneMeldekortBehandling.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KunneIkkeUnderkjenneMeldekortBehandling.BegrunnelseMåVæreUtfylt -> HttpStatusCode.BadRequest to ErrorJson(
        "Begrunnelse må være utfylt",
        "begrunnelse_må_være_utfylt",
    )

    KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErAlleredeBesluttet -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er allerede besluttet",
        "behandlingen_er_besluttet",
    )

    KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErIkkeKlarTilBeslutning -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke klar til beslutning",
        "behandlingen_er_ikke_klar_til_beslutning",
    )

    KunneIkkeUnderkjenneMeldekortBehandling.SaksbehandlerKanIkkeUnderkjenneSinEgenBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Du kan ikke underkjenne din egen behandling",
        "kan_ikke_underkjenne_egen_behandling",
    )
}
