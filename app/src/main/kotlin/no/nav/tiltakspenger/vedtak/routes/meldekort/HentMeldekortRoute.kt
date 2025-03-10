package no.nav.tiltakspenger.vedtak.routes.meldekort

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respond404NotFound
import no.nav.tiltakspenger.saksbehandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.fantIkkeMeldekort
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.vedtak.routes.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.vedtak.routes.withSakId

private const val PATH = "/sak/{sakId}/meldeperiode/{kjedeId}"

fun Route.hentMeldekortRoute(
    sakService: SakService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    get(PATH) {
        logger.debug { "Mottatt get-request på $PATH" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldeperiodeKjedeId { kjedeId ->
                    val correlationId = call.correlationId()

                    val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId = correlationId).getOrElse {
                        when (it) {
                            is KunneIkkeHenteSakForSakId.HarIkkeTilgang -> call.respond403Forbidden(
                                ikkeTilgang(
                                    "Må ha en av rollene ${it.kreverEnAvRollene} for å hente meldekort",
                                ),
                            )
                        }
                        return@withMeldeperiodeKjedeId
                    }

                    val meldeperiodeKjedeDTO =
                        sak.toMeldeperiodeKjedeDTO(kjedeId = kjedeId)
                            ?: return@withMeldeperiodeKjedeId call.respond404NotFound(fantIkkeMeldekort())

                    auditService.logMedSakId(
                        sakId = sakId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.ACCESS,
                        contextMessage = "Henter meldekort",
                        correlationId = correlationId,
                    )

                    // TODO post-mvp jah: Saksbehandlerne reagerte på ordet saksperiode og ønsket seg "vedtaksperiode". Gitt at man har en forlengelse vil man ha et førstegangsvedtak+forlengelsesvedtak. Ønsker de ikke se den totale meldeperioden for den gitte saken?
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = meldeperiodeKjedeDTO,
                    )
                }
            }
        }
    }
}
