package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.ugyldigRequest
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.tilOppdaterKommandoStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdaterMeldekortdagDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortbehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KunneIkkeForhåndsviseBrevMeldekortbehandling
import java.time.LocalDate

private const val PATH =
    "/sak/{sakId}/meldekortbehandling/{meldekortId}/forhandsvis"

private data class ForhåndsvisBrevMeldekortbehandlingBody(
    val tekstTilVedtaksbrev: String?,
    val meldeperioder: List<OppdatertMeldeperiodeDTO>,
)

private data class LegacyForhåndsvisBrevMeldekortbehandlingBody(
    val tekstTilVedtaksbrev: String?,
    val dager: List<LegacyOppdaterMeldekortdagDTO>,
    val versjon: Int = 1,
) {
    data class LegacyOppdaterMeldekortdagDTO(
        val dato: LocalDate,
        val status: MeldekortDagStatusDTO,
    )

    fun tilNyBody(kjedeId: String): ForhåndsvisBrevMeldekortbehandlingBody {
        return ForhåndsvisBrevMeldekortbehandlingBody(
            tekstTilVedtaksbrev = tekstTilVedtaksbrev,
            meldeperioder = listOf(
                OppdatertMeldeperiodeDTO(
                    kjedeId = kjedeId,
                    dager = dager.map {
                        OppdaterMeldekortdagDTO(
                            dato = it.dato,
                            status = it.status,
                        )
                    },
                ),
            ),
        )
    }
}

private fun deserializeCompatForhåndsvisBrevMeldekortbehandlingBody(
    rawBody: String,
    hentLegacyKjedeId: () -> String,
): ForhåndsvisBrevMeldekortbehandlingBody {
    return runCatching {
        deserialize<ForhåndsvisBrevMeldekortbehandlingBody>(rawBody)
    }.getOrElse {
        deserialize<LegacyForhåndsvisBrevMeldekortbehandlingBody>(rawBody).tilNyBody(hentLegacyKjedeId())
    }
}

fun Route.forhåndsvisBrevMeldekortbehandlingRoute(
    forhåndsvisBrevMeldekortbehandlingService: ForhåndsvisBrevMeldekortbehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - saksbehandler ønsker å forhåndsvise brev" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                val rawBody = call.receiveText()
                val body = runCatching {
                    deserializeCompatForhåndsvisBrevMeldekortbehandlingBody(
                        rawBody = rawBody,
                        hentLegacyKjedeId = {
                            forhåndsvisBrevMeldekortbehandlingService.hentKjedeIdForMeldekortbehandling(meldekortId)
                        },
                    )
                }.getOrElse {
                    call.respond400BadRequest(errorJson = ugyldigRequest())
                    return@withMeldekortId
                }

                forhåndsvisBrevMeldekortbehandlingService.forhåndsvisBrev(
                    command = ForhåndsvisBrevMeldekortbehandlingCommand(
                        meldekortbehandlingId = meldekortId,
                        correlationId = correlationId,
                        saksbehandler = saksbehandler,
                        tekstTilVedtaksbrev = body.tekstTilVedtaksbrev?.toNonBlankString(),
                        meldeperioder = body.meldeperioder.map {
                            OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode(
                                it.dager.map { dag ->
                                    OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag(
                                        dag = dag.dato,
                                        status = dag.status.tilOppdaterKommandoStatus(),
                                    )
                                }.toNonEmptyListOrNull()!!,
                                kjedeId = MeldeperiodeKjedeId(it.kjedeId),
                            )
                        },
                    ),
                ).fold(
                    ifLeft = {
                        call.respondJson(statusAndValue = it.tilStatusOgErrorJson())
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

private fun KunneIkkeForhåndsviseBrevMeldekortbehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KunneIkkeForhåndsviseBrevMeldekortbehandling.FeilVedGenereringAvPdf -> Pair(
            HttpStatusCode.InternalServerError,
            ErrorJson(
                "Feil ved generering av PDF. Feilen er blitt logget. Vennligst prøv igjen senere.",
                "feil_ved_generering_av_pdf",
            ),
        )

        KunneIkkeForhåndsviseBrevMeldekortbehandling.FantIkkeMeldekortbehandling -> Pair(
            HttpStatusCode.NotFound,
            ErrorJson(
                "Fant ikke meldekortbehandling.",
                "fant_ikke_meldekortbehandling",
            ),
        )
    }
}
