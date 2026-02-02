package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSaksnummer
import java.time.Clock

fun Route.hentSakForSaksnummerRoute(
    sakService: SakService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    get("$SAK_PATH/{saksnummer}") {
        logger.debug { "Mottatt get-request på $SAK_PATH/{saksnummer}" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@get
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSaksnummer { saksnummer ->
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

            val sak = try {
                sakService.hentForSaksnummer(saksnummer)
            } catch (e: Exception) {
                when (e) {
                    is IkkeFunnetException -> {
                        logger.warn { "Fant ikke sak med saksnummer $saksnummer" }
                        call.respond(
                            HttpStatusCode.NotFound,
                            "Fant ikke sak med saksnummer $saksnummer",
                        )
                        return@withSaksnummer
                    }

                    else -> {
                        logger.error(e) { "Uventet feil ved henting av sak for saksnummer $saksnummer" }
                        throw e
                    }
                }
            }

            tilgangskontrollService.harTilgangTilPerson(sak.fnr, token, saksbehandler)

            auditService.logMedSaksnummer(
                saksnummer = saksnummer,
                navIdent = saksbehandler.navIdent,
                action = AuditLogEvent.Action.ACCESS,
                contextMessage = "Henter hele saken til brukeren",
                correlationId = call.correlationId(),
            )

            call.respondJson(value = sak.toSakDTO(clock))
        }
    }
}
