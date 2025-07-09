package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KunneIkkeOppdatereBarnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.SøknadsbehandlingTilBeslutningBody
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilStatusOgErrorJson
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

internal const val BARNETILLEGG_PATH = "/sak/{sakId}/behandling/{behandlingId}/barnetillegg"

fun Route.oppdaterBarnetilleggRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterBarnetilleggService: OppdaterBarnetilleggService,
) {
    val logger = KotlinLogging.logger {}
    patch(BARNETILLEGG_PATH) {
        logger.debug { "Mottatt patch-request på $BARNETILLEGG_PATH - oppdaterer barnetillegg" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<SøknadsbehandlingTilBeslutningBody> { body ->
                        val correlationId = call.correlationId()
                        val toDomain = body.toDomain(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        )

                        if (toDomain !is SendSøknadsbehandlingTilBeslutningKommando.Innvilgelse) {
                            call.respond(
                                status = HttpStatusCode.BadRequest,
                                ErrorJson(
                                    melding = "Oppdatering av barnetillegg krever at resultatet av en søknadsbehandling er innvilgelse.",
                                    kode = "søknadsbehandling_må_være_innvilgelse",
                                ),
                            )
                        } else {
                            oppdaterBarnetilleggService.oppdaterBarnetillegg(toDomain).fold(
                                ifLeft = {
                                    val (status, errorJson) = it.tilStatusOgErrorJson()
                                    call.respond(status = status, errorJson)
                                },
                                ifRight = {
                                    auditService.logMedBehandlingId(
                                        behandlingId = behandlingId,
                                        navIdent = saksbehandler.navIdent,
                                        action = AuditLogEvent.Action.UPDATE,
                                        contextMessage = "Oppdaterer barnetillegg",
                                        correlationId = correlationId,
                                    )
                                    call.respond(status = HttpStatusCode.OK, it.tilBehandlingDTO())
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun KunneIkkeOppdatereBarnetillegg.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KunneIkkeOppdatereBarnetillegg.KunneIkkeOppdatereBehandling -> this.valideringsfeil.tilStatusOgErrorJson()
}
