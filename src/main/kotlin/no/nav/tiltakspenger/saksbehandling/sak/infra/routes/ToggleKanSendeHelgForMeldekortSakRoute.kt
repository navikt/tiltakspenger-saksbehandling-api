package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import java.time.Clock

private const val TOGGLE_HELG_MELDEKORT_PATH = "$SAK_PATH/{sakId}/toggle-helg-meldekort"

private data class ToggleKanSendeHelgForMeldekortBody(val kanSendeHelg: Boolean)

fun Route.toggleKanSendeHelgForMeldekortSakRoute(
    sakService: SakService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(TOGGLE_HELG_MELDEKORT_PATH) {
        logger.debug { "Mottatt get-request på $TOGGLE_HELG_MELDEKORT_PATH" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post

        call.withSakId { sakId ->
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

            call.withBody<ToggleKanSendeHelgForMeldekortBody> { body ->
                auditService.logMedSakId(
                    sakId = sakId,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.UPDATE,
                    contextMessage = "Oppdaterer brukerens mulighet til å melde helg",
                    correlationId = call.correlationId(),
                )
                sakService.oppdaterKanSendeInnHelgForMeldekort(
                    sakId = sakId,
                    kanSendeHelg = body.kanSendeHelg,
                )
                    .also { sak ->
                        call.respondJson(value = sak.toSakDTO(clock))
                    }
            }
        }
    }
}
