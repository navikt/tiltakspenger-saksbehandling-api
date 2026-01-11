package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import java.time.Clock

private const val PATH = "sak/{sakId}/meldeperiode/{kjedeId}/opprettBehandling"

/** Brukes både for å opprette en ny meldekortbehandling, og for å ta opp en meldekortbehandling som har blitt lagt tilbake
 *
 *  TODO abn: burde splitte denne og tilhørende service-funksjon for å separere de to bruksområdene bedre
 * */
fun Route.opprettMeldekortBehandlingRoute(
    opprettMeldekortBehandlingService: OpprettMeldekortBehandlingService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - oppretter meldekort-behandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            val ki = call.parameters["kjedeId"]
            call.withMeldeperiodeKjedeId { kjedeId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                opprettMeldekortBehandlingService.opprettBehandling(
                    kjedeId = kjedeId,
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                ).fold(
                    {
                        when (it) {
                            is KanIkkeOppretteMeldekortbehandling.HenteNavKontorFeilet -> call.respond500InternalServerError(
                                melding = "Kunne ikke hente Nav-kontor for brukeren",
                                kode = "kunne_ikke_hente_navkontor",
                            )

                            is KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil -> call.respond400BadRequest(
                                melding = "Meldeperiodekjeden er i en tilstand som ikke tillater å opprette en behandling: ${it.feil}",
                                kode = it.feil.toString(),
                            )
                        }
                    },
                    { (sak) ->
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.CREATE,
                            contextMessage = "Oppretter meldekort-behandling",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = sak.toMeldeperiodeKjedeDTO(kjedeId, clock))
                    },
                )
            }
        }
    }
}
