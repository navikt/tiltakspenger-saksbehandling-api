package no.nav.tiltakspenger.saksbehandling.person.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SAK_PATH

fun Route.hentPersonRoute(
    sakService: SakService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    get("$SAK_PATH/{sakId}/personopplysninger") {
        logger.debug { "Mottatt get-request på '$SAK_PATH/{sakId}/personopplysninger' - henter personopplysninger for en sak" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            val correlationId = call.correlationId()
            sakService.hentEnkelPersonForSakId(sakId, saksbehandler, correlationId).map {
                it.toEnkelPersonDTO()
            }.fold(
                {
                    when (it) {
                        KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl -> call.respond500InternalServerError(
                            melding = "Feil ved kall mot PDL",
                            kode = "feil_ved_kall_mot_pdl",
                        )
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
