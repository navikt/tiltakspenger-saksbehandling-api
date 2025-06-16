package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond501NotImplemented
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSøknadId

fun Route.startSøknadsbehandlingRoute(
    tokenService: TokenService,
    startSøknadsbehandlingService: StartSøknadsbehandlingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/soknad/{søknadId}/startbehandling") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/soknad/{søknadId}/startbehandling' - Starter behandlingen og knytter til søknad og sak. Knytter også saksbehandleren til behandlingen." }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withSøknadId { søknadId ->
                    val correlationId = call.correlationId()
                    startSøknadsbehandlingService.startSøknadsbehandling(
                        søknadId = søknadId,
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ).fold(
                        {
                            when (it) {
                                is KanIkkeStarteSøknadsbehandling.OppretteBehandling ->
                                    when (it.underliggende) {
                                        is KanIkkeOppretteBehandling.IngenRelevanteTiltak -> call.respond501NotImplemented(
                                            Standardfeil.ikkeImplementert(
                                                "Ingen relevante tiltak for denne søknaden - dette støtter vi ikke ennå",
                                            ),
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
                            call.respond(HttpStatusCode.OK, it.tilSøknadsbehandlingDTO())
                        },
                    )
                }
            }
        }
    }
}
