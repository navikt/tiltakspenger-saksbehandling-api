package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forvaltningsmelding

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.KanIkkeSendeForvaltningsmelding
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.tilKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.toStatusAndErrorJson
import no.nav.tiltakspenger.saksbehandling.klage.service.SendForvaltningsmeldingKlagebehandlingService

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/forvaltningsmelding"

fun Route.sendForvaltningsmeldingRoute(
    sendForvaltningsmeldingKlagebehandlingService: SendForvaltningsmeldingKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Sender forvaltningsmelding for klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<ForvaltningsmeldingBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    sendForvaltningsmeldingKlagebehandlingService.sendForvaltningsmelding(
                        kommando = body.tilKommando(
                            sakId = sakId,
                            klagebehandlingId = klagebehandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(it.tilStatusOgErrorJson())
                        },
                        ifRight = { (_, behandling) ->
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Sender forvaltningsmelding for klagebehandling på sak $sakId",
                                correlationId = correlationId,
                                behandlingId = behandling.id,
                            )
                            call.respondJson(value = behandling.tilKlagebehandlingDTO())
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeSendeForvaltningsmelding.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KanIkkeSendeForvaltningsmelding.ForvaltningsmeldingErAlleredeSendt -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Det er allerede sendt en forvaltningsmelding for denne klagebehandlingen",
                "forvaltningsmelding_allerede_sendt",
            ),
        )

        KanIkkeSendeForvaltningsmelding.ManglerTekst -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Forvaltningsmeldingen må ha minst én tekstblokk",
                "forvaltningsmelding_mangler_tekst",
            ),
        )

        is KanIkkeSendeForvaltningsmelding.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(this.forventetSaksbehandler),
        )

        is KanIkkeSendeForvaltningsmelding.KanIkkeOppdateres -> this.underliggende.toStatusAndErrorJson()
    }
}
