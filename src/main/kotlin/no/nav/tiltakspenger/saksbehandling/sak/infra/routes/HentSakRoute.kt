package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.UgyldigFnrException
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
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
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.person.infra.route.FnrDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock

/**
 *  Brukes i hovedsak til å slå opp sak på fnr fra frontend, men vi støtter også søk på saksnummer eller sak-id
 * */
fun Route.søkFnrSaksnummerOgSakIdRoute(
    sakService: SakService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    post(SAK_PATH) {
        logger.debug { "Mottatt post-request på $SAK_PATH" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withBody<FnrDTO> { body ->
            val fnrEllerSakIdEllerSaksnummer = body.fnr

            val sak: Sak? = Either.catch { Fnr.fromString(fnrEllerSakIdEllerSaksnummer) }.fold(
                ifRight = { fnr ->
                    sakService.hentForFnr(fnr)
                },
                ifLeft = {
                    Either.catch {
                        return@fold sakService.hentForSakId(SakId.fromString(fnrEllerSakIdEllerSaksnummer))
                    }

                    Either.catch {
                        return@fold sakService.hentForSaksnummer(Saksnummer(fnrEllerSakIdEllerSaksnummer))
                    }

                    when (it) {
                        is UgyldigFnrException -> call.respond400BadRequest(
                            melding = "Forventer at fødselsnummeret er 11 siffer",
                            kode = "ugyldig_fnr",
                        )

                        else -> call.respond500InternalServerError(
                            melding = "Ukjent feil ved lesing av fødselsnummeret",
                            kode = "fnr_parsing_feil",
                        )
                    }
                    return@withBody
                },
            )

            if (sak == null) {
                call.respond404NotFound(Standardfeil.fantIkkeFnr())
                return@withBody
            }

            val correlationId = call.correlationId()
            val fnr = sak.fnr

            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
            tilgangskontrollService.harTilgangTilPerson(fnr, token, saksbehandler)

            auditService.logMedBrukerId(
                brukerId = fnr,
                navIdent = saksbehandler.navIdent,
                action = AuditLogEvent.Action.SEARCH,
                contextMessage = "Søker opp alle sakene til brukeren",
                correlationId = correlationId,
            )

            call.respond(message = sak.toSakDTO(clock), status = HttpStatusCode.OK)
        }
    }
}
