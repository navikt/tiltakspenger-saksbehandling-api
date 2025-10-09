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
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SAK_PATH

fun Route.hentBarnRoute(
    sakService: SakService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    get("$SAK_PATH/{sakId}/personopplysninger/barn") {
        logger.debug { "Mottatt get-request på '$SAK_PATH/{sakId}/personopplysninger/barn' - henter personopplysninger om barna for en sak" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            val correlationId = call.correlationId()
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
            sakService.hentBarnForSakId(sakId, correlationId).map {
                it.map { barn -> barn.toEnkelPersonDTO() }
            }.fold(
                {
                    when (it) {
                        KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl -> call.respond500InternalServerError(
                            melding = "Feil ved kall mot PDL",
                            kode = "feil_ved_kall_mot_pdl",
                        )
                        KunneIkkeHenteEnkelPerson.FeilVedKallMotSkjerming -> call.respond500InternalServerError(
                            melding = "Feil ved kall mot skjermingstjenesten",
                            kode = "feil_ved_kall_mot_skjerming",
                        )
                    }
                },
                { barn ->
                    auditService.logMedSakId(
                        sakId = sakId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.ACCESS,
                        contextMessage = "Henter barn for en sak",
                        correlationId = correlationId,
                    )
                    call.respond(status = HttpStatusCode.OK, barn)
                },
            )
        }
    }
}
