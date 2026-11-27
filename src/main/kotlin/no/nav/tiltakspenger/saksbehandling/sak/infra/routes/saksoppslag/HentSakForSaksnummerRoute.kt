package no.nav.tiltakspenger.saksbehandling.sak.infra.routes.saksoppslag

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.infra.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.verifisertAzurePrincipal
import no.nav.tiltakspenger.saksbehandling.infra.route.withSaksnummer
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SAK_PATH
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SakDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

typealias Saksoppslagstilgang = suspend ApplicationCall.() -> (suspend (Sak) -> SakDTO)?

typealias OpprettSaksoppslagstilgang = (
    tilgangskontrollService: TilgangskontrollService,
    auditService: AuditService,
    clock: Clock,
) -> Saksoppslagstilgang

fun saksbehandlertilgangForSaksoppslag(
    tilgangskontrollService: TilgangskontrollService,
    auditService: AuditService,
    clock: Clock,
): Saksoppslagstilgang = tilgang@{
    val saksbehandler = saksbehandler(autoriserteBrukerroller()) ?: return@tilgang null
    val token = verifisertAzurePrincipal().token

    { sak ->
        tilgangskontrollService.harTilgangTilPerson(sak.fnr, token, saksbehandler)
        auditService.logMedSaksnummer(
            saksnummer = sak.saksnummer,
            navIdent = saksbehandler.navIdent,
            action = AuditLogEvent.Action.ACCESS,
            contextMessage = "Henter hele saken til brukeren",
            correlationId = correlationId(),
        )
        sak.toSakDTO(saksbehandler, clock)
    }
}

fun Route.hentSakForSaksnummerRoute(
    sakService: SakService,
    saksoppslagstilgang: Saksoppslagstilgang,
) {
    val sti = "$SAK_PATH/{saksnummer}"
    val logger = KotlinLogging.logger {}
    get(sti) {
        logger.debug { "Mottatt get-request på $sti" }
        val lagSvar = call.saksoppslagstilgang() ?: return@get
        call.withSaksnummer { saksnummer ->
            val sak = try {
                sakService.hentForSaksnummer(saksnummer)
            } catch (_: IkkeFunnetException) {
                logger.warn { "Fant ikke sak med saksnummer $saksnummer" }
                call.respond(HttpStatusCode.NotFound, "Fant ikke sak med saksnummer $saksnummer")
                return@withSaksnummer
            }

            call.respondJson(value = lagSvar(sak))
        }
    }
}
