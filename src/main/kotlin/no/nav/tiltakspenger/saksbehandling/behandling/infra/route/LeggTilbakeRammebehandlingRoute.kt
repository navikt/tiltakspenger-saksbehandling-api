package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.KanIkkeLeggeTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake.toStatusAndErrorJson

private const val LEGG_TILBAKE_BEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/legg-tilbake"

fun Route.leggTilbakeRammebehandlingRoute(
    auditService: AuditService,
    leggTilbakeBehandlingService: LeggTilbakeRammebehandlingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(LEGG_TILBAKE_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$LEGG_TILBAKE_BEHANDLING_PATH' - Fjerner saksbehandler/beslutter fra behandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                leggTilbakeBehandlingService.leggTilbakeRammebehandling(
                    sakId,
                    behandlingId,
                    saksbehandler,
                ).fold(
                    ifLeft = { feil ->
                        call.loggOgSvarFeil(
                            logger = logger,
                            operasjon = "Legg tilbake rammebehandling",
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
                            contextMessage = "Saksbehandler fjernes fra behandlingen",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}

fun KanIkkeLeggeTilbakeRammebehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeLeggeTilbakeRammebehandling.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandler for å legge tilbake denne behandlingen.",
        "maa_vaere_saksbehandler",
    )

    KanIkkeLeggeTilbakeRammebehandling.MåVæreSaksbehandlerForBehandlingen -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandleren som er tildelt behandlingen for å legge den tilbake.",
        "maa_vaere_saksbehandler_for_behandlingen",
    )

    KanIkkeLeggeTilbakeRammebehandling.MåVæreBeslutter -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være beslutter for å legge tilbake denne behandlingen.",
        "maa_vaere_beslutter",
    )

    KanIkkeLeggeTilbakeRammebehandling.MåVæreBeslutterForBehandlingen -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være beslutteren som er tildelt behandlingen for å legge den tilbake.",
        "maa_vaere_beslutter_for_behandlingen",
    )

    is KanIkkeLeggeTilbakeRammebehandling.UgyldigStatus -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke legge tilbake behandling med status ${this.status}.",
        "ugyldig_status_for_legg_tilbake",
    )

    is KanIkkeLeggeTilbakeRammebehandling.FeilVedKlagebehandling -> this.originalfeil.toStatusAndErrorJson()
}
