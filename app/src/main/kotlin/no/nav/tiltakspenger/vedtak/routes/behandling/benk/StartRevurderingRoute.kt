package no.nav.tiltakspenger.vedtak.routes.behandling.benk

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.domene.behandling.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.service.sak.KanIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.service.sak.StartRevurderingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.ErrorJson
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBody
import no.nav.tiltakspenger.vedtak.routes.withSakId

/**
 * TODO post-mvp jah: Vi trenger denne bl.a. pga. statisitkk. Feltene kan modnes litt før vi tar en diskusjon med fag.
 * @param type Revurderingstype. Mulige verdier: "OMGJØRING_ETTER_KLAGE", "OMGJØRING_ETTER_EGET_TILTAK"
 */
private data class StartRevurderingBody(
    val periode: PeriodeDTO,
    // val type: String,
) {
    fun tilKommando(
        sakId: SakId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): StartRevurderingKommando {
        return StartRevurderingKommando(
            sakId = sakId,
            periode = periode.toDomain(),
            correlationId = correlationId,
            saksbehandler = saksbehandler,
        )
    }
}

fun Route.startRevurderingRoute(
    tokenService: TokenService,
    startRevurderingService: StartRevurderingService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}

    post("/sak/{sakId}/revurdering") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/revurdering' - Oppretter en ny revurdering" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBody<StartRevurderingBody> { body ->
                    val correlationId = call.correlationId()
                    startRevurderingService.startRevurdering(body.tilKommando(sakId, correlationId, saksbehandler))
                        .fold(
                            {
                                when (it) {
                                    is KanIkkeStarteRevurdering.HarIkkeTilgang -> {
                                        call.respond403Forbidden(
                                            ikkeTilgang("Krever en av rollene ${it.kreverEnAvRollene} for å starte en behandling."),
                                        )
                                    }

                                    is KanIkkeStarteRevurdering.KanIkkeStanseUtbetaltDag -> {
                                        call.respond403Forbidden(
                                            ErrorJson(
                                                melding = "Kan ikke stanse utbetalt dag. Første mulige stansdato er ${it.førsteMuligeStansdato}, ønsket stansdato er ${it.ønsketStansdato}",
                                                kode = "kan_ikke_stanse_utbetalt_dag",
                                            ),
                                        )
                                    }
                                }
                            },
                            {
                                val revurderingId = it.second.id
                                auditService.logMedSakId(
                                    sakId = sakId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.CREATE,
                                    contextMessage = "Oppretter revurdering på sak $sakId",
                                    correlationId = correlationId,
                                    behandlingId = revurderingId,
                                )
                                call.respond(HttpStatusCode.OK, BehandlingIdDTO(revurderingId.toString()))
                            },
                        )
                }
            }
        }
    }
}
