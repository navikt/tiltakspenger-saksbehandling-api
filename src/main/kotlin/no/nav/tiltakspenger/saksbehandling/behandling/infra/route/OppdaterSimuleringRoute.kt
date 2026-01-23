package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterSimuleringService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import java.time.Clock

private const val OPPDATER_SIMULERING_PATH = "/sak/{sakId}/behandling/{behandlingId}/oppdaterSimulering"

fun Route.oppdaterSimuleringRoute(
    oppdaterSimuleringService: OppdaterSimuleringService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    post(OPPDATER_SIMULERING_PATH) {
        logger.debug { "Mottatt post-request på $OPPDATER_SIMULERING_PATH - saksbehandler prøver og oppdatere simuleringen på en behandling under behandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            // Kommentar jah: Dette kan både være en MeldekortId eller BehandlingId, burde kanskje hatt en fellestype for disse?
            val behandlingId = UlidBase(call.parameters["behandlingId"]!!)
            val correlationId = call.correlationId()

            krevSaksbehandlerRolle(saksbehandler)
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
            oppdaterSimuleringService.oppdaterSimulering(
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).fold(
                ifLeft = {
                    when (it) {
                        KunneIkkeSimulere.Stengt -> call.respondJson(
                            status = HttpStatusCode.ServiceUnavailable,
                            value = ErrorJson(
                                "Økonomisystemet er stengt. Typisk åpningstider er mellom 6 og 21 på hverdager og visse lørdager.",
                                "økonomisystemet_er_stengt",
                            ),
                        )

                        KunneIkkeSimulere.UkjentFeil -> call.respondJson(
                            status = HttpStatusCode.InternalServerError,
                            value = ErrorJson(
                                "Ukjent feil ved simulering",
                                "ukjent_feil_ved_simulering",
                            ),
                        )

                        KunneIkkeSimulere.Timeout -> call.respondJson(
                            status = HttpStatusCode.RequestTimeout,
                            value = ErrorJson(
                                "Tjenesten for simulering svarte ikke (time-out). Du kan prøve igjen.",
                                "timeout_ved_simulering",
                            ),
                        )
                    }
                },
                ifRight = { (sak, behandling) ->
                    behandling.fold(
                        ifLeft = {
                            auditService.logMedBehandlingId(
                                behandlingId = it.id,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Saksbehandler har oppdatert simuleringen på en rammebehandling under behandling",
                                correlationId = correlationId,
                            )
                            call.respondJson(value = sak.tilRammebehandlingDTO(it.id))
                        },
                        ifRight = {
                            auditService.logMedMeldekortId(
                                meldekortId = it.id,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Saksbehandler har oppdatert simuleringen på en meldekortbehandling under behandling",
                                correlationId = correlationId,
                            )
                            call.respondJson(value = sak.toMeldeperiodeKjedeDTO(it.kjedeId, clock))
                        },
                    )
                },
            )
        }
    }
}
