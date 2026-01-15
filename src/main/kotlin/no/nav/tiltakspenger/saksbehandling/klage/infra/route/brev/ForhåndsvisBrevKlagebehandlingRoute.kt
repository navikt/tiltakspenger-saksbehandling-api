package no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.`KanIkkeForhåndsviseBrev`
import no.nav.tiltakspenger.saksbehandling.klage.service.ForhåndsvisBrevKlagebehandlingService

internal const val FORHÅNDSVIS_BREV_KLAGEBEHANDLING_PATH =
    "/sak/{sakId}/klage/{klagebehandlingId}/forhandsvis"

fun Route.forhåndsvisBrevKlagebehandlingRoute(
    forhåndsvisBrevKlagebehandlingService: ForhåndsvisBrevKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(FORHÅNDSVIS_BREV_KLAGEBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på $FORHÅNDSVIS_BREV_KLAGEBEHANDLING_PATH - saksbehandler ønsker å forhåndsvise brev" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<KlagebehandlingTeksterTilBrevBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    forhåndsvisBrevKlagebehandlingService.forhåndsvisBrev(
                        kommando = body.tilKommando(
                            sakId = sakId,
                            klagebehandlingId = klagebehandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(valueAndStatus = it.tilStatusOgErrorJson())
                        },
                        ifRight = {
                            auditService.logMedSakId(
                                sakId = sakId,
                                behandlingId = klagebehandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Forhåndsviser brev for klagebehandling",
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
        is KanIkkeForhåndsviseBrev.FeilMotPdfgen -> Pair(
            HttpStatusCode.InternalServerError,
            ErrorJson(
                "Feil ved generering av PDF. Feilen er blitt logget. Vennligst prøv igjen senere.",
                "feil_ved_generering_av_pdf",
            ),
        )
    }
}
