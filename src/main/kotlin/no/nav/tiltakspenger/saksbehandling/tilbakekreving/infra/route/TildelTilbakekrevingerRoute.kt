package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.route

import arrow.core.getOrElse
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.parseBody
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.service.TilbakekrevingBehandlingTildelingService
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.service.TildelTilbakekrevingerKommando

private const val TILDEL_TILBAKEKREVINGER_PATH = "/tilbakekreving/tildel"

private data class RequestBody(
    val tilbakekrevinger: List<TilbakekrevingMedSakIdBody>,
) {
    fun tilKommando(saksbehandler: Saksbehandler): TildelTilbakekrevingerKommando? {
        return TildelTilbakekrevingerKommando(
            saksbehandler = saksbehandler,
            tilbakekrevinger = tilbakekrevinger.map {
                TildelTilbakekrevingerKommando.TilbakekrevingMedSakId(
                    tilbakekrevingId = TilbakekrevingId.fromString(it.tilbakekrevingId),
                    sakId = SakId.fromString(it.sakId),
                )
            }.toNonEmptyListOrNull() ?: return null,
        )
    }

    data class TilbakekrevingMedSakIdBody(
        val tilbakekrevingId: String,
        val sakId: String,
    )
}

private data class ResponseBody(
    val tilbakekrevinger: List<TilbakekrevingRespons>,
) {
    data class TilbakekrevingRespons(
        val tilbakekrevingId: String,
        val saksnummer: String,
    )
}

/** Tildeler saksbehandler eller beslutter flere tilbakekrevinger på én gang, fra benken. */
fun Route.tildelTilbakekrevingerRoute(
    auditService: AuditService,
    tilbakekrevingBehandlingTildelingService: TilbakekrevingBehandlingTildelingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    post(TILDEL_TILBAKEKREVINGER_PATH) {
        logger.debug { "Mottatt post-request på '$TILDEL_TILBAKEKREVINGER_PATH' - Knytter saksbehandler/beslutter til flere tilbakekrevingsbehandlinger." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post

        val body = call.parseBody<RequestBody>().getOrElse {
            call.respond400BadRequest(it)
            return@post
        }

        val kommando = body.tilKommando(saksbehandler) ?: run {
            call.respondJson(
                status = HttpStatusCode.BadRequest,
                value = ErrorJson(
                    melding = "Du må sende inn minst én tilbakekreving.",
                    kode = "må_ha_minst_en_tilbakekreving",
                ),
            )
            return@post
        }

        val correlationId = call.correlationId()
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        tilgangskontrollService.harTilgangTilPersonerForSakIder(
            sakIder = kommando.hentSakIder(),
            saksbehandler = saksbehandler,
            saksbehandlerToken = token,
        )

        val tildelte = tilbakekrevingBehandlingTildelingService.tildelBehandlinger(kommando)

        tildelte.forEach { (sak, tilbakekreving) ->
            auditService.logMedSakId(
                sakId = sak.id,
                navIdent = saksbehandler.navIdent,
                action = AuditLogEvent.Action.UPDATE,
                contextMessage = "Saksbehandler tar tilbakekrevingsbehandlingen(e)",
                correlationId = correlationId,
                behandlingId = tilbakekreving.id,
            )
        }

        call.respondJson(
            value = ResponseBody(
                tildelte.map { (sak, tilbakekreving) ->
                    ResponseBody.TilbakekrevingRespons(
                        tilbakekrevingId = tilbakekreving.id.toString(),
                        saksnummer = sak.saksnummer.toString(),
                    )
                },
            ),
        )
    }
}
