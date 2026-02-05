package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.server.auth.principal
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisRammevedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

/**
 * Brukes for søknadsbehandling (innvilgelse+avslag) + revurdering (innvilgelse+stans).
 */
fun Route.forhåndsvisVedtaksbrevRoute(
    auditService: AuditService,
    forhåndsvisVedtaksbrevService: ForhåndsvisRammevedtaksbrevService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/forhandsvis") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/behandling/{behandlingId}/forhandsvis' - forhåndsviser vedtaksbrev" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                call.withBody<ForhåndsvisVedtaksbrevRequestBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    forhåndsvisVedtaksbrevService.forhåndsvisVedtaksbrev(
                        body.toDomain(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).also {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "forhåndsviser vedtaksbrev",
                            correlationId = correlationId,
                        )
                        call.respondBytes(it.getContent(), ContentType.Application.Pdf)
                    }
                }
            }
        }
    }
}
