package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.RammebehandlingId
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ta.KunneIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingerKommando
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta.toStatusAndErrorJson
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SakDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val TA_RAMMEBEHANDLINGER_PATH = "/behandlinger/ta"

private data class RequestBody(
    val behandlinger: List<BehandlingMedSakIdBody>,
    val returnerSaker: Boolean = false,
) {
    fun tilKommando(saksbehandler: Saksbehandler): Either<KunneIkkeTaBehandling, TaRammebehandlingerKommando> {
        return TaRammebehandlingerKommando(
            saksbehandler = saksbehandler,
            behandlinger = behandlinger.map {
                TaRammebehandlingerKommando.RammebehandlingMedSakId(
                    behandlingId = RammebehandlingId.fromString(it.behandlingId),
                    sakId = SakId.fromString(it.sakId),
                )
            }.toNonEmptyListOrNull() ?: return KunneIkkeTaBehandling.MåTaMinimumEnRammebehandling.left(),
        ).right()
    }

    data class BehandlingMedSakIdBody(
        val behandlingId: String,
        val sakId: String,
    )
}

private data class ResponseBody(
    val behandlinger: List<BehandlingRespons>,
    val saker: List<SakDTO>,
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
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    post(TA_RAMMEBEHANDLINGER_PATH) {
        logger.debug { "Mottatt post-request på '$TA_RAMMEBEHANDLINGER_PATH' - Knytter saksbehandler/beslutter til flere behandlinger." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post

        val body = call.parseBody<RequestBody>().getOrElse {
            call.respond400BadRequest(it)
            return@post
        }

        val kommando = body.tilKommando(saksbehandler).getOrElse {
            call.respondJson(statusAndValue = it.tilStatusOgErrorJson())
            return@post
        }

        val correlationId = call.correlationId()
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        tilgangskontrollService.harTilgangTilPersonerForSakIder(
            kommando.hentSakIder(),
            saksbehandler = saksbehandler,
            saksbehandlerToken = token,
        )

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
            ifRight = {
                it.forEach {
                    auditService.logMedRammebehandlingId(
                        behandlingId = it.second.id,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = "Saksbehandler tar behandlingen(e)",
                        correlationId = correlationId,
                    )
                }
                call.respondJson(
                    ResponseBody(
                        it.map { (sak, behandling) ->
                            ResponseBody.BehandlingRespons(
                                behandlingId = behandling.id.toString(),
                                saksnummer = sak.saksnummer.toString(),
                            )
                        },
                        saker = if (body.returnerSaker) {
                            it.distinctBy { (sak) -> sak.id }.map { (sak) -> sak.toSakDTO(saksbehandler, clock) }
                        } else {
                            emptyList()
                        },
                    ),
                )
            },
        )
    }
}

fun KunneIkkeTaBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KunneIkkeTaBehandling.BehandlingenErIEnTilstandSomIkkeTillaterÅTaBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er i en tilstand som ikke tillater å ta behandlingen.",
        "behandlingen_er_i_en_tilstand_som_ikke_tillater_å_ta_behandling",
    )

    KunneIkkeTaBehandling.BehandlingenHarEksisterendeBeslutter -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen har allerede en beslutter.",
        "behandlingen_har_allerede_en_beslutter",
    )

    KunneIkkeTaBehandling.BehandlingenHarEksisterendeSaksbehandler -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen har allerede en saksbehandler.",
        "behandlingen_har_allerede_en_saksbehandler",
    )

    is KunneIkkeTaBehandling.FeilVedKlagebehandling -> this.originalfeil.toStatusAndErrorJson()

    KunneIkkeTaBehandling.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandler for å ta denne behandlingen.",
        "maa_vaere_saksbehandler",
    )

    KunneIkkeTaBehandling.MåVæreBeslutter -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være beslutter for å ta denne behandlingen.",
        "maa_vaere_beslutter",
    )

    KunneIkkeTaBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSammePåBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Saksbehandler og beslutter kan ikke være den samme på behandlingen.",
        "saksbehandler_og_beslutter_kan_ikke_være_den_samme_på_behandlingen",
    )

    KunneIkkeTaBehandling.MåTaMinimumEnRammebehandling -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Du må sende inn minst en behandling.",
        kode = "må_ha_minst_en_behandling",
    )
}
