package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respond501NotImplemented
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeBehandleSøknadPåNytt
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSøknadId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil

fun Route.behandleSøknadPåNyttRoute(
    behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/soknad/{søknadId}/behandling/ny-behandling") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/soknad/{søknadId}/ny-behandling' - Starter behandling for søknad på nytt og knytter til søknad og sak. Knytter også saksbehandleren til behandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withSøknadId { søknadId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                behandleSøknadPåNyttService.startSøknadsbehandlingPåNytt(
                    søknadId = søknadId,
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                ).fold(
                    {
                        when (it) {
                            is KanIkkeBehandleSøknadPåNytt.OppretteBehandling ->
                                when (it.underliggende) {
                                    KanIkkeOppretteBehandling.IngenRelevanteTiltak -> call.respond501NotImplemented(
                                        Standardfeil.ikkeImplementert(
                                            "Ingen relevante tiltak for denne søknaden - dette støtter vi ikke ennå",
                                        ),
                                    )
                                }
                        }
                    },
                    { (sak, søknadsbehandling) ->
                        auditService.logForSøknadId(
                            søknadId = søknadId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.CREATE,
                            contextMessage = "Oppretter behandling fra søknad på nytt og starter behandlingen",
                            correlationId = correlationId,
                        )
                        call.respond(
                            HttpStatusCode.OK,
                            søknadsbehandling.tilSøknadsbehandlingDTO(
                                meldeperiodeBeregninger = sak.meldeperiodeBeregninger,
                                utbetalingsstatus = null,
                                utbetalinger = sak.utbetalinger,
                            ),
                        )
                    },
                )
            }
        }
    }
}
