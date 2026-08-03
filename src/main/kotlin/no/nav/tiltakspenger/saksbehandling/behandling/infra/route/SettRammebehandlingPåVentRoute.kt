package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.KanIkkeSetteRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock
import java.time.LocalDate

private const val SETT_BEHANDLING_PÅ_VENT_PATH = "/sak/{sakId}/behandling/{behandlingId}/pause"

private data class SettPåVentBody(
    val begrunnelse: String,
    val frist: LocalDate?,
) {
    fun toKommando(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ) = SettRammebehandlingPåVentKommando(
        sakId = sakId,
        rammebehandlingId = behandlingId,
        begrunnelse = begrunnelse,
        frist = frist,
        saksbehandler = saksbehandler,
    )
}

fun Route.settRammebehandlingPåVentRoute(
    auditService: AuditService,
    settBehandlingPåVentService: SettRammebehandlingPåVentService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    post(SETT_BEHANDLING_PÅ_VENT_PATH) {
        logger.debug { "Mottatt post-request på '$SETT_BEHANDLING_PÅ_VENT_PATH' - Setter behandling på vent inntil videre." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                call.withBody<SettPåVentBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    settBehandlingPåVentService.settBehandlingPåVent(
                        body.toKommando(sakId, behandlingId, saksbehandler),
                    ).fold(
                        ifLeft = { feil ->
                            call.loggOgSvarFeil(
                                logger = logger,
                                operasjon = "Sett rammebehandling på vent",
                                feil = feil,
                                statusOgErrorJson = feil.tilStatusOgErrorJson(),
                                kontekst = "sakId=$sakId, behandlingId=$behandlingId",
                            )
                        },
                        ifRight = { (sak) ->
                            auditService.logMedRammebehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Setter rammebehandling på vent",
                                correlationId = correlationId,
                            )

                            call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeSetteRammebehandlingPåVent.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeSetteRammebehandlingPåVent.BehandlingenErAlleredePåVent -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er allerede satt på vent.",
        "behandlingen_er_allerede_paa_vent",
    )

    KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandler for å sette denne behandlingen på vent.",
        "maa_vaere_saksbehandler",
    )

    KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandlerForBehandlingen -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandleren som er tildelt behandlingen for å sette den på vent.",
        "maa_vaere_saksbehandler_for_behandlingen",
    )

    KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutter -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være beslutter for å sette denne behandlingen på vent.",
        "maa_vaere_beslutter",
    )

    KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutterForBehandlingen -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være beslutteren som er tildelt behandlingen for å sette den på vent.",
        "maa_vaere_beslutter_for_behandlingen",
    )

    is KanIkkeSetteRammebehandlingPåVent.UgyldigStatus -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke sette behandling med status $status på vent.",
        "ugyldig_status_for_sett_paa_vent",
    )

    is KanIkkeSetteRammebehandlingPåVent.KunneIkkeSetteKlagebehandlingenPåVent -> HttpStatusCode.BadRequest to ErrorJson(
        "Klagebehandlingen som henger på behandlingen kunne ikke settes på vent.",
        "kunne_ikke_sette_klagebehandlingen_paa_vent",
    )
}
