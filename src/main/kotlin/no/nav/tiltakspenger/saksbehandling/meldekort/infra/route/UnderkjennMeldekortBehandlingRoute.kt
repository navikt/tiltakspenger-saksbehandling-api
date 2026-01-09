package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KunneIkkeUnderkjenneMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UnderkjennMeldekortBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService

internal const val UNDERKJENN_MELDEKORT_BEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/underkjenn"

private data class UnderkjennMeldekortBehandlingBody(val begrunnelse: String)

fun Route.underkjennMeldekortBehandlingRoute(
    underkjennMeldekortBehandlingService: UnderkjennMeldekortBehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(UNDERKJENN_MELDEKORT_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på $UNDERKJENN_MELDEKORT_BEHANDLING_PATH - Beslutter ønsker å underkjenne" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                call.withBody<UnderkjennMeldekortBehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    underkjennMeldekortBehandlingService.underkjenn(
                        UnderkjennMeldekortBehandlingCommand(
                            sakId = sakId,
                            meldekortId = meldekortId,
                            begrunnelse = body.begrunnelse,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(valueAndStatus = it.toErrorJson())
                        },
                        ifRight = { (sak, behandling) ->
                            auditService.logMedMeldekortId(
                                meldekortId = meldekortId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Beslutter underkjenner meldekort $meldekortId",
                                correlationId = correlationId,
                            )

                            call.respondJson(
                                value = behandling.tilMeldekortBehandlingDTO(beregninger = sak.meldeperiodeBeregninger),
                            )
                        },
                    )
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

    KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErIkkeUnderBeslutning -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke under beslutning",
        "behandlingen_er_ikke_klar_til_beslutning",
    )

    KunneIkkeUnderkjenneMeldekortBehandling.SaksbehandlerKanIkkeUnderkjenneSinEgenBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Du kan ikke underkjenne din egen behandling",
        "kan_ikke_underkjenne_egen_behandling",
    )

    KunneIkkeUnderkjenneMeldekortBehandling.MåVæreBeslutterForMeldekortet -> HttpStatusCode.BadRequest to ErrorJson(
        "Du kan ikke underkjenne meldekortet da du ikke er beslutter for denne meldekortbehandlingen",
        "må_være_beslutter_for_meldekortet",
    )
}
