package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.ErrorJsonMedData
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilErrorJson
import no.nav.tiltakspenger.saksbehandling.vedtak.OpprettRammevedtakFeil

private val logger = KotlinLogging.logger {}

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/iverksett"

fun Route.iverksettRammebehandlingRoute(
    iverksettRammebehandlingService: IverksettRammebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - iverksetter behandlingen, oppretter vedtak, evt. genererer meldekort og asynkront sender brev." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                iverksettRammebehandlingService.iverksettRammebehandling(
                    rammebehandlingId = behandlingId,
                    beslutter = saksbehandler,
                    sakId = sakId,
                    correlationId = correlationId,
                ).fold(
                    { call.handleIverksettFeil(it) },
                    { (sak) ->
                        auditService.logMedSakId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Beslutter iverksetter behandling $behandlingId",
                            correlationId = correlationId,
                            sakId = sakId,
                        )
                        call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}

private suspend fun ApplicationCall.handleIverksettFeil(feil: KanIkkeIverksetteBehandling) {
    when (feil) {
        is KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter -> respond400BadRequest(
            behandlingenEiesAvAnnenSaksbehandler(feil.eiesAvBeslutter),
        )

        is KanIkkeIverksetteBehandling.SimuleringFeil -> respondJson(feil.feil.tilSimuleringErrorJson())

        is KanIkkeIverksetteBehandling.UtbetalingFeil -> respondJson(
            feil.feil.tilErrorJson().let { (status, errorJson) ->
                status to ErrorJsonMedData(
                    melding = errorJson.melding,
                    kode = errorJson.kode,
                    data = feil.sak.tilRammebehandlingDTO(feil.behandling.id),
                )
            },
        )

        is KanIkkeIverksetteBehandling.OpprettVedtakFeil -> {
            logger.error { "Kunne ikke opprette rammevedtak ved iverksetting: ${feil.feil}" }
            respondJson(feil.feil.tilErrorJson())
        }
    }
}

private fun OpprettRammevedtakFeil.tilErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is OpprettRammevedtakFeil.RammebehandlingIkkeVedtatt -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Rammebehandlingen må være vedtatt for å kunne iverksettes. Status: $status.",
        kode = "rammebehandling_ikke_vedtatt",
    )

    is OpprettRammevedtakFeil.UgyldigKlagebehandlingStatus -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Tilknyttet klagebehandling må være VEDTATT eller FERDIGSTILT for å kunne iverksette rammebehandlingen. Status: $status.",
        kode = "ugyldig_klagebehandling_status",
    )

    is OpprettRammevedtakFeil.UgyldigOmgjøring -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Behandlingen har ugyldige omgjøringer. Saken kan ha blitt endret av et nytt vedtak etter at behandlingen ble sendt til godkjenning, og må sendes tilbake for å vurderes på nytt.",
        kode = "ugyldig_omgjøring",
    )
}
