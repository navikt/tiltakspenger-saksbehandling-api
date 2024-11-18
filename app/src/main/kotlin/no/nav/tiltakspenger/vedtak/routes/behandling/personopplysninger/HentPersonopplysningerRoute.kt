package no.nav.tiltakspenger.vedtak.routes.behandling.personopplysninger

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.saksbehandling.service.person.KunneIkkeHenteEnkelPerson
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond500InternalServerError
import no.nav.tiltakspenger.vedtak.routes.sak.SAK_PATH
import no.nav.tiltakspenger.vedtak.routes.withSakId

fun Route.hentPersonRoute(
    tokenService: TokenService,
    sakService: SakService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    get("$SAK_PATH/{sakId}/personopplysninger") {
        logger.debug("Mottatt get-request på '$SAK_PATH/{sakId}/personopplysninger' - henter personopplysninger for en sak")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                val correlationId = call.correlationId()
                sakService.hentEnkelPersonForSakId(sakId, saksbehandler, correlationId).map {
                    it.toDTO()
                }.fold(
                    {
                        when (it) {
                            KunneIkkeHenteEnkelPerson.FantIkkeSakId -> Standardfeil.fantIkkeSak()
                            KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl -> call.respond500InternalServerError(
                                melding = "Feil ved kall mot PDL",
                                kode = "feil_ved_kall_mot_pdl",
                            )
                            is KunneIkkeHenteEnkelPerson.HarIkkeTilgang -> call.respond403Forbidden(ikkeTilgang("Må ha rollen ${it.kreverEnAvRollene} for å hente personopplysninger knyttet til sak"))
                        }
                    },
                    { personopplysninger ->
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "Henter personopplysninger for en sak",
                            correlationId = correlationId,
                        )
                        call.respond(status = HttpStatusCode.OK, personopplysninger)
                    },
                )
            }
        }
    }
}
