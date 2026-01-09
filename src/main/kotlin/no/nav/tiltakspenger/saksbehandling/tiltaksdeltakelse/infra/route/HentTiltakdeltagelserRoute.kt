package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SAK_PATH
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service.KunneIkkeHenteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service.TiltaksdeltakelseService
import java.time.LocalDate

fun Route.hentTiltakdeltakelserRoute(
    tiltaksdeltakelseService: TiltaksdeltakelseService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    get("$SAK_PATH/{sakId}/tiltaksdeltakelser") {
        logger.debug { "Mottatt get-request på '$SAK_PATH/{sakId}/tiltaksdeltakelser' - henter tiltaksdeltakelser for en sak" }
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@get
        call.withSakId { sakId ->
            val correlationId = call.correlationId()
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

            val fraOgMed = call.request.queryParameters["fraOgMed"]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val tilOgMed = call.request.queryParameters["tilOgMed"]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

            tiltaksdeltakelseService.hentTiltaksdeltakelserForSak(
                sakId = sakId,
                fraOgMed = fraOgMed,
                tilOgMed = tilOgMed,
                correlationId = correlationId,
            )
                .map { tiltaksdeltakelser -> tiltaksdeltakelser.map { deltakelse -> deltakelse.toDTO() } }
                .fold(
                    {
                        when (it) {
                            KunneIkkeHenteTiltaksdeltakelser.FeilVedKallMotPdl -> call.respond500InternalServerError(
                                melding = "Feil ved kall mot PDL",
                                kode = "feil_ved_kall_mot_pdl",
                            )

                            KunneIkkeHenteTiltaksdeltakelser.OppslagsperiodeMangler -> call.respond400BadRequest(
                                melding = "Oppslagsperiode mangler",
                                kode = "oppslagsperiode_mangler",
                            )

                            KunneIkkeHenteTiltaksdeltakelser.NegativOppslagsperiode -> call.respond400BadRequest(
                                melding = "Til og med dato er etter fra og med datoen",
                                kode = "negativ_periode",
                            )
                        }
                    },
                    { tiltaksdeltakelser ->
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "Henter tiltaksdeltakelser for en sak",
                            correlationId = correlationId,
                        )
                        call.respondJson(value = tiltaksdeltakelser)
                    },
                )
        }
    }
}
