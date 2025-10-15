package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
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
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSaksnummer
import java.time.Clock

private const val TOGGLE_HELG_MELDEKORT_PATH = "$SAK_PATH/{saksnummer}/toggle-helg-meldekort"

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

        call.withSaksnummer { saksnummer ->
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, token)

            call.withBody<ToggleKanSendeHelgForMeldekortBody> { body ->
                auditService.logMedSaksnummer(
                    saksnummer = saksnummer,
                    navIdent = saksbehandler.navIdent,
                    action = AuditLogEvent.Action.ACCESS,
                    contextMessage = "Henter hele saken til brukeren",
                    correlationId = call.correlationId(),
                )
                sakService.oppdaterKanSendeInnHelgForMeldekort(
                    saksnummer = saksnummer,
                    kanSendeHelg = body.kanSendeHelg,
                )
                    .also { sak ->
                        call.respond(message = sak.toSakDTO(clock), status = HttpStatusCode.OK)
                    }
            }
        }
    }
}
