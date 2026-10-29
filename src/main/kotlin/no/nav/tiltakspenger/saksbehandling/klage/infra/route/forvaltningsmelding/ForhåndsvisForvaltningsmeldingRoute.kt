package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forvaltningsmelding

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KanIkkeForhåndsviseBrev
import no.nav.tiltakspenger.saksbehandling.klage.service.ForhåndsvisForvaltningsmeldingKlagebehandlingService

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/forvaltningsmelding/forhandsvis"

fun Route.forhåndsvisForvaltningsmeldingRoute(
    forhåndsvisForvaltningsmeldingKlagebehandlingService: ForhåndsvisForvaltningsmeldingKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - saksbehandler ønsker å forhåndsvise forvaltningsmelding" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        if (skalSladdeFor(saksbehandler)) {
            call.respond403Forbidden(Standardfeil.pdfKreverFagrolle())
            return@post
        }
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<ForvaltningsmeldingBody> { body ->
                    val correlationId = call.correlationId()
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    forhåndsvisForvaltningsmeldingKlagebehandlingService.forhåndsvis(
                        kommando = body.tilKommando(
                            sakId = sakId,
                            klagebehandlingId = klagebehandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(statusAndValue = it.tilStatusOgErrorJson())
                        },
                        ifRight = {
                            auditService.logMedSakId(
                                sakId = sakId,
                                behandlingId = klagebehandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Forhåndsviser forvaltningsmelding for klagebehandling",
                                correlationId = correlationId,
                            )
                            call.respondBytes(it.getContent(), ContentType.Application.Pdf)
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeForhåndsviseBrev.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeForhåndsviseBrev.FeilMotPdfgenrs -> Pair(
            HttpStatusCode.InternalServerError,
            ErrorJson(
                "Feil ved generering av PDF. Feilen er blitt logget. Vennligst prøv igjen senere.",
                "feil_ved_generering_av_pdf",
            ),
        )
    }
}
