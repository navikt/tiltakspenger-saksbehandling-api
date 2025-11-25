package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId

fun Route.hentEllerOpprettSakRoute(
    sakService: SakService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    personService: PersonService,
) {
    val logger = KotlinLogging.logger {}

    put(SAK_PATH) {
        logger.debug { "Mottatt kall på '$SAK_PATH' - henter eller oppretter sak." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@put
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@put
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
        val correlationId = call.correlationId()
        val fnr = Fnr.fromString(call.receive<FnrDTO>().fnr)
        tilgangskontrollService.harTilgangTilPerson(
            fnr = fnr,
            saksbehandlerToken = token,
            saksbehandler = saksbehandler,
        )
        personService.hentEnkelPersonFnr(fnr).fold(
            ifLeft = {
                call.respond(HttpStatusCode.NotFound, "Fant ikke person")
            },
            ifRight = {
                val (sak, opprettet) = sakService.hentEllerOpprettSak(
                    fnr = fnr,
                    correlationId = correlationId,
                )
                auditService.logMedSakId(
                    sakId = sak.id,
                    navIdent = saksbehandler.navIdent,
                    action = if (opprettet) AuditLogEvent.Action.CREATE else AuditLogEvent.Action.ACCESS,
                    contextMessage = "Hentet eller opprettet sak.",
                    correlationId = correlationId,
                )

                call.respond(
                    message = HentEllerOpprettSakResponse(saksnummer = sak.saksnummer.verdi, opprettet = opprettet),
                    status = HttpStatusCode.OK,
                )
            },
        )
    }
}

data class HentEllerOpprettSakResponse(
    val saksnummer: String,
    val opprettet: Boolean,
)
