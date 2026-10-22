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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.hentTilbake.KanIkkeHenteTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/henttilbake"

fun Route.hentTilbakeRammebehandlingRoute(
    auditService: AuditService,
    hentTilbakeRammebehandlingService: HentTilbakeRammebehandlingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Saksbehandler henter tilbake en automatisk behandlet rammebehandling." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post

        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, saksbehandlerToken = token)

                hentTilbakeRammebehandlingService.hentTilbakeRammebehandling(
                    sakId,
                    behandlingId,
                    saksbehandler,
                ).fold(
                    ifLeft = { feil ->
                        call.loggOgSvarFeil(
                            logger = logger,
                            operasjon = "Hente tilbake rammebehandling",
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
                            contextMessage = "Saksbehandler henter tilbake behandlingen",
                            correlationId = correlationId,
                        )
                        call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                    },
                )
            }
        }
    }
}

fun KanIkkeHenteTilbakeRammebehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeHenteTilbakeRammebehandling.UgyldigStatus -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Kan ikke hente tilbake behandling med status $status.",
        kode = "behandlingen_må_være_klar_til_beslutning",
    )

    KanIkkeHenteTilbakeRammebehandling.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        melding = "Du må ha saksbehandlerrollen for å hente tilbake behandlingen.",
        kode = "må_være_saksbehandler",
    )

    KanIkkeHenteTilbakeRammebehandling.MåVæreSøknadsbehandling -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Kun søknadsbehandlinger kan hentes tilbake.",
        kode = "behandlingen_må_være_søknadsbehandling",
    )

    KanIkkeHenteTilbakeRammebehandling.ErIkkeAutomatiskBehandlet -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Behandlingen er ikke automatisk behandlet og kan ikke hentes tilbake.",
        kode = "behandlingen_må_være_automatisk_behandlet",
    )

    KanIkkeHenteTilbakeRammebehandling.BehandlingenErSattPåVent -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Behandlingen er satt på vent.",
        kode = "behandlingen_kan_ikke_være_satt_på_vent",
    )

    KanIkkeHenteTilbakeRammebehandling.BehandlingenErEndret -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Behandlingen er endret av noen andre. Last siden på nytt.",
        kode = "behandlingen_er_endret",
    )
}
