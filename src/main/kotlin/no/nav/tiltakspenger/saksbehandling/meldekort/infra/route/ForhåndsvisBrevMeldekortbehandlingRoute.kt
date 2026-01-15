package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
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
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortbehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KunneIkkeForhåndsviseBrevMeldekortBehandling
import java.time.LocalDate

internal const val FORHÅNDSVIS_BREV_MELDEKORTBEHANDLING_PATH =
    "/sak/{sakId}/meldekortbehandling/{meldekortId}/forhandsvis"

private data class ForhåndsvisBrevMeldekortbehandlingBody(
    val tekstTilVedtaksbrev: String?,
    val dager: List<Dag>?,
) {
    data class Dag(
        val dato: LocalDate,
        val status: OppdaterMeldekortKommando.Status,
    )
}

fun Route.forhåndsvisBrevMeldekortbehandlingRoute(
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
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    forhåndsvisBrevMeldekortBehandlingService.forhåndsvisBrev(
                        command = ForhåndsvisBrevMeldekortbehandlingCommand(
                            meldekortbehandlingId = meldekortId,
                            correlationId = correlationId,
                            saksbehandler = saksbehandler,
                            tekstTilVedtaksbrev = body.tekstTilVedtaksbrev?.toNonBlankString(),
                            // copy-pasta av Oppdater
                            dager = body.dager?.let {
                                OppdaterMeldekortKommando.Dager(
                                    it.map { dag ->
                                        OppdaterMeldekortKommando.Dager.Dag(
                                            dag = dag.dato,
                                            status = dag.status,
                                        )
                                    }.toNonEmptyListOrNull()!!,
                                )
                            },
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(valueAndStatus = it.tilStatusOgErrorJson())
                        },
                        ifRight = {
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Forhåndsviser brev for meldekortbehandling",
                                correlationId = correlationId,
                                behandlingId = meldekortId,
                            )
                            call.respondBytes(it.pdf.getContent(), ContentType.Application.Pdf)
                        },
                    )
                }
            }
        }
    }
}

internal fun KunneIkkeForhåndsviseBrevMeldekortBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KunneIkkeForhåndsviseBrevMeldekortBehandling.FeilVedGenereringAvPdf -> Pair(
            HttpStatusCode.InternalServerError,
            ErrorJson(
                "Feil ved generering av PDF. Feilen er blitt logget. Vennligst prøv igjen senere.",
                "feil_ved_generering_av_pdf",
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
}
