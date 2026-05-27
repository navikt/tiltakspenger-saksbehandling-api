package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.withMeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingTypeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "sak/{sakId}/meldeperiode/{kjedeId}/opprettBehandling"

private data class OpprettMeldekortbehandlingBody(
    val type: MeldekortbehandlingTypeDTO? = null,
    val v2: Boolean = false,
)

fun Route.opprettMeldekortbehandlingRoute(
    opprettMeldekortbehandlingService: OpprettMeldekortbehandlingService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - oppretter meldekort-behandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldeperiodeKjedeId { kjedeId ->
                val rawBody = call.receiveText()
                val body = if (rawBody.isBlank()) {
                    OpprettMeldekortbehandlingBody()
                } else {
                    try {
                        deserialize<OpprettMeldekortbehandlingBody>(rawBody)
                    } catch (e: Exception) {
                        logger.debug(e) { "Kunne ikke deserialisere request-body" }
                        call.respond400BadRequest(
                            melding = "Kunne ikke deserialisere request",
                            kode = "ugyldig_request",
                        )
                        return@withMeldeperiodeKjedeId
                    }
                }

                if (body.v2 && body.type == null) {
                    call.respond400BadRequest(
                        melding = "type må være satt når v2=true",
                        kode = "type_mangler",
                    )
                    return@withMeldeperiodeKjedeId
                }

                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                opprettMeldekortbehandlingService.opprettBehandling(
                    kjedeId = kjedeId,
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                ).fold(
                    {
                        when (it) {
                            is KanIkkeOppretteMeldekortbehandling.HenteNavKontorFeilet -> call.respond500InternalServerError(
                                melding = "Kunne ikke hente Nav-kontor for brukeren",
                                kode = "kunne_ikke_hente_navkontor",
                            )

                            is KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil -> call.respond400BadRequest(
                                melding = "Meldeperiodekjeden er i en tilstand som ikke tillater å opprette en behandling: ${it.feil}",
                                kode = it.feil.toString(),
                            )
                        }
                    },
                    { (sak, behandling) ->
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.CREATE,
                            contextMessage = "Oppretter meldekort-behandling",
                            correlationId = correlationId,
                            behandlingId = behandling.id,
                        )

                        if (body.v2) {
                            call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                        } else {
                            call.respondJson(value = sak.toMeldeperiodeKjedeDTO(kjedeId, clock))
                        }
                    },
                )
            }
        }
    }
}
