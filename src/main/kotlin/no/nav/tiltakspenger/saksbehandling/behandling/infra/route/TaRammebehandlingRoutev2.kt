package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import arrow.core.toNonEmptyListOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingMedSakId
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingerKommando
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil

private const val TA_RAMMEBEHANDLINGER_PATH = "/behandlinger/ta"

private data class RequestBody(
    val behandlinger: List<BehandlingMedSakIdBody>,
) {
    fun tilKommando(saksbehandler: Saksbehandler): TaRammebehandlingerKommando {
        return TaRammebehandlingerKommando(
            saksbehandler = saksbehandler,
            behandlinger = behandlinger.map {
                RammebehandlingMedSakId(
                    behandlingId = RammebehandlingId.fromString(it.behandlingId),
                    sakId = SakId.fromString(it.sakId),
                )
            }.toNonEmptyListOrThrow(),
        )
    }

    data class BehandlingMedSakIdBody(
        val behandlingId: String,
        val sakId: String,
    )
}

private data class ResponsBody(
    val behandlinger: List<BehandlingRespons>,
) {
    data class BehandlingRespons(
        val behandlingId: String,
        val saksnummer: String,
    )
}

fun Route.taRammebehandlingerRoute(
    auditService: AuditService,
    taRammebehandlingService: TaRammebehandlingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(TA_RAMMEBEHANDLINGER_PATH) {
        logger.debug { "Mottatt post-request på '$TA_RAMMEBEHANDLINGER_PATH' - Knytter saksbehandler/beslutter til flere behandlinger." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withBody<RequestBody> { body ->

            val kommando = body.tilKommando(saksbehandler)

            val correlationId = call.correlationId()
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

            tilgangskontrollService.harTilgangTilPersonForSakIder(kommando.behandlinger.map { it.sakId }.toNonEmptySet(), saksbehandler = saksbehandler, saksbehandlerToken = token)

            taRammebehandlingService.taRammebehandlinger(kommando = kommando).fold(
                ifLeft = { feil ->
                    call.loggOgSvarFeil(
                        logger = logger,
                        operasjon = "Tildele en eller flere rammebehandlinger",
                        feil = feil,
                        statusOgErrorJson = feil.tilStatusOgErrorJson(),
                        kontekst = "Behandlinger som ble forsøkt å ta: ${kommando.behandlinger}",
                    )
                },
                ifRight = { behandlinger ->
                    behandlinger.forEach {
                        auditService.logMedRammebehandlingId(
                            behandlingId = it.id,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler tar behandlingen(e)",
                            correlationId = correlationId,
                        )
                    }
                    call.respondJson(ResponsBody(behandlinger.map { ResponsBody.BehandlingRespons(it.id.toString(), it.saksnummer.toString()) }))
                },
            )
        }
    }
}
