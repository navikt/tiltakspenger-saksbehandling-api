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
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import java.time.Clock

private const val PATH = "sak/{sakId}/meldeperiode/{kjedeId}/opprettBehandling"

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
                            is KanIkkeOppretteMeldekortBehandling.HenteNavkontorFeilet -> call.respond500InternalServerError(
                                melding = "Kunne ikke hente Nav-kontor for brukeren",
                                kode = "kunne_ikke_hente_navkontor",
                            )

                            is KanIkkeOppretteMeldekortBehandling.KanIkkeOpprettePåKjede -> call.respond400BadRequest(
                                melding = "Meldeperiodekjeden er ikke i en tilstand som tillater å opprette en behandling",
                                kode = "mel",
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

                        call.respond(HttpStatusCode.OK, it.toMeldeperiodeKjedeDTO(kjedeId, clock))
                    },
                )
            }
        }
    }
}
