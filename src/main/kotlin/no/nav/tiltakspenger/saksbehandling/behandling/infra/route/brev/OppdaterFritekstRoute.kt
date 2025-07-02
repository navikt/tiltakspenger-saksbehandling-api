package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereFritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilStatusOgErrorJson
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private data class FritekstBody(
    val fritekst: String,
) {
    fun toDomain() = FritekstTilVedtaksbrev(saniter(fritekst))
}

private const val FRITEKST_ROUTE = "/sak/{sakId}/behandling/{behandlingId}/fritekst"

fun Route.oppdaterFritekstTilVedtaksbrevRoute(
    tokenService: TokenService,
    auditService: AuditService,
    oppdaterFritekstTilVedtaksbrevService: OppdaterFritekstTilVedtaksbrevService,
) {
    val logger = KotlinLogging.logger {}
    patch(FRITEKST_ROUTE) {
        logger.debug { "Mottatt get-request på '$FRITEKST_ROUTE' - oppdaterer fritekst til vedtaksbrev" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withBehandlingId { behandlingId ->
                    call.withBody<FritekstBody> { body ->
                        val correlationId = call.correlationId()
                        oppdaterFritekstTilVedtaksbrevService.oppdaterFritekstTilVedtaksbrev(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            fritekstTilVedtaksbrev = body.toDomain(),
                        ).fold(
                            ifLeft = {
                                val (status, errorJson) = it.tilStatusOgErrorJson()
                                call.respond(status = status, errorJson)
                            },
                            ifRight = {
                                auditService.logMedBehandlingId(
                                    behandlingId = behandlingId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Oppdaterer fritekst til vedtaksbrev",
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

internal fun KunneIkkeOppdatereFritekstTilVedtaksbrev.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> =
    when (this) {
        is KunneIkkeOppdatereFritekstTilVedtaksbrev.KunneIkkeOppdatereBehandling -> this.valideringsfeil.tilStatusOgErrorJson()
    }
