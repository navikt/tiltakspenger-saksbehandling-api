package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta.KanIkkeTaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.TaMeldekortbehandlingService

private const val PATH = "/sak/{sakId}/meldekort/{meldekortId}/ta"

fun Route.taMeldekortbehandlingRoute(
    auditService: AuditService,
    taMeldekortbehandlingService: TaMeldekortbehandlingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Knytter saksbehandler/beslutter til meldekortbehandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                taMeldekortbehandlingService.taMeldekortbehandling(
                    sakId = sakId,
                    meldekortId = meldekortId,
                    saksbehandler = saksbehandler,
                ).fold(
                    {
                        when (it) {
                            is KanIkkeTaMeldekortbehandling.MeldekortbehandlingFinnesIkke -> call.respond404NotFound(
                                melding = "Fant ikke meldekortbehandling $meldekortId på sak $sakId",
                                kode = "meldekortbehandling_finnes_ikke",
                            )

                            is KanIkkeTaMeldekortbehandling.HarAlleredeSaksbehandler -> call.respond400BadRequest(
                                melding = "Meldekortbehandlingen har allerede en saksbehandler. Bruk overta for å overta behandlingen.",
                                kode = "behandlingen_har_allerede_saksbehandler",
                            )

                            is KanIkkeTaMeldekortbehandling.HarAlleredeBeslutter -> call.respond400BadRequest(
                                melding = "Meldekortbehandlingen har allerede en beslutter. Bruk overta for å overta behandlingen.",
                                kode = "behandlingen_har_allerede_beslutter",
                            )

                            is KanIkkeTaMeldekortbehandling.BeslutterKanIkkeVæreSammeSomSaksbehandler -> call.respond400BadRequest(
                                melding = "Beslutter kan ikke være den samme som saksbehandleren på meldekortbehandlingen",
                                kode = "beslutter_kan_ikke_vaere_samme_som_saksbehandler",
                            )

                            is KanIkkeTaMeldekortbehandling.UgyldigStatus -> call.respond400BadRequest(
                                melding = "Kan ikke ta meldekortbehandling med status ${it.status}",
                                kode = "ugyldig_status_for_ta",
                            )
                        }
                    },
                    { (sak, behandling) ->
                        auditService.logMedMeldekortId(
                            meldekortId = meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler/beslutter tar meldekortbehandlingen",
                            correlationId = correlationId,
                        )

                        call.respondJson(
                            value = behandling.tilMeldekortbehandlingDTO(beregninger = sak.meldeperiodeBeregninger),
                        )
                    },
                )
            }
        }
    }
}
