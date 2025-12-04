package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortbehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KunneIkkeForhåndsviseBrevMeldekortBehandling

internal const val FORHÅNDSVIS_BREV_MELDEKORTBEHANDLING_PATH =
    "/sak/{sakId}/meldekortbehandling/{meldekortId}/forhandsvis"

private data class ForhåndsvisBrevMeldekortbehandlingBody(
    val tekstTilVedtaksbrev: String?,
)

fun Route.forhåndsvisBrevMeldekortbehandling(
    forhåndsvisBrevMeldekortBehandlingService: ForhåndsvisBrevMeldekortBehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(FORHÅNDSVIS_BREV_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på $FORHÅNDSVIS_BREV_MELDEKORTBEHANDLING_PATH - saksbehandler ønsker å forhåndsvise brev" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                call.withBody<ForhåndsvisBrevMeldekortbehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    forhåndsvisBrevMeldekortBehandlingService.forhåndsvisBrev(
                        command = ForhåndsvisBrevMeldekortbehandlingCommand(
                            meldekortbehandlingId = meldekortId,
                            correlationId = correlationId,
                            saksbehandler = saksbehandler,
                            tekstTilVedtaksbrev = body.tekstTilVedtaksbrev?.toNonBlankString(),
                        ),
                    ).fold(
                        ifLeft = {
                            val (status, error) = it.tilStatusOgErrorJson()
                            call.respond(status, error)
                        },
                        ifRight = {
                            auditService.logMedMeldekortId(
                                meldekortId = meldekortId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Forhåndsviser brev for meldekortbehandling",
                                correlationId = correlationId,
                            )
                            call.respondBytes(it.pdf.getContent(), ContentType.Application.Pdf)
                        },
                    )
                }
            }
        }
    }
}

internal fun KunneIkkeForhåndsviseBrevMeldekortBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> =
    when (this) {
        is KunneIkkeForhåndsviseBrevMeldekortBehandling.FeilVedGenereringAvPdf -> this.feil.tilStatusOgErrorJson()
        KunneIkkeForhåndsviseBrevMeldekortBehandling.BehandlingMåHaBeregningForÅForhåndsviseBrev -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Behandlingen må beregnes først, før du kan forhåndsvise brev.",
                "behandlingen_ma_beregnes_først",
            ),
        )

        KunneIkkeForhåndsviseBrevMeldekortBehandling.FantIkkeMeldekortbehandling -> Pair(
            HttpStatusCode.NotFound,
            ErrorJson(
                "Fant ikke meldekortbehandling.",
                "fant_ikke_meldekortbehandling",
            ),
        )
    }

internal fun KunneIkkeGenererePdf.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = Pair(
    HttpStatusCode.InternalServerError,
    ErrorJson(
        "Feil ved generering av PDF. Feilen er blitt logget. Vennligst prøv igjen senere.",
        "feil_ved_generering_av_pdf",
    ),
)
