package no.nav.tiltakspenger.vedtak.routes.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppretteBehandling.FantIkkeTiltak
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppretteBehandling.StøtterIkkeBarnetillegg
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppretteBehandling.StøtterKunInnvilgelse
import no.nav.tiltakspenger.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.service.sak.KanIkkeStarteSøknadsbehandling
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.benk.BehandlingIdDTO
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.fantIkkeTiltak
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.støtterIkkeBarnetillegg
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.støtterIkkeDelvisEllerAvslag
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond400BadRequest
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond500InternalServerError
import no.nav.tiltakspenger.vedtak.routes.withBody

/**
 * TODO John og Anders: Denne slettes etter vi har laget ny, forenklet vilkårsvurdering.
 */
fun Route.startBehandlingRoute(
    tokenService: TokenService,
    behandlingService: BehandlingService,
    auditService: AuditService,
    søknadService: SøknadService,
) {
    val logger = KotlinLogging.logger {}
    post("$BEHANDLING_PATH/startbehandling") {
        logger.debug { "Mottatt post-request på '$BEHANDLING_PATH/startbehandling' - Starter behandlingen og knytter til sak. Knytter også saksbehandleren til behandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            // TODO post-mvp jah: Kan ikke søknadId ligge i pathen?
            call.withBody<BehandlingIdDTO> { body ->
                val søknadId = SøknadId.fromString(body.id)
                val correlationId = call.correlationId()
                val sakId = søknadService.hentSakIdForSoknad(søknadId)
                behandlingService.startFørstegangsbehandling(
                    søknadId,
                    sakId,
                    saksbehandler,
                    correlationId = correlationId,
                ).fold(
                    {
                        when (it) {
                            is KanIkkeStarteSøknadsbehandling.HarAlleredeStartetBehandlingen -> {
                                call.respond(HttpStatusCode.OK, BehandlingIdDTO(it.behandlingId.toString()))
                            }

                            is KanIkkeStarteSøknadsbehandling.OppretteBehandling ->
                                when (it.underliggende) {
                                    FantIkkeTiltak ->
                                        call.respond500InternalServerError(fantIkkeTiltak())

                                    StøtterIkkeBarnetillegg ->
                                        call.respond400BadRequest(støtterIkkeBarnetillegg())

                                    is StøtterKunInnvilgelse -> call.respond400BadRequest(støtterIkkeDelvisEllerAvslag())
                                }

                            is KanIkkeStarteSøknadsbehandling.HarIkkeTilgang -> {
                                call.respond403Forbidden(
                                    ikkeTilgang("Krever en av rollene ${it.kreverEnAvRollene} for å starte en behandling."),
                                )
                            }
                        }
                    },
                    {
                        auditService.logForSøknadId(
                            søknadId = søknadId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.CREATE,
                            contextMessage = "Oppretter behandling fra søknad og starter behandlingen",
                            correlationId = correlationId,
                        )

                        call.respond(HttpStatusCode.OK, BehandlingIdDTO(it.førstegangsbehandling!!.id.toString()))
                    },
                )
            }
        }
    }
}
