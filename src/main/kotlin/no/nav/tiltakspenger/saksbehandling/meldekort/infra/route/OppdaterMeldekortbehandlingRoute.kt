package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.json.deserialize
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling.MeldekortperiodenKanIkkeVæreFremITid
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortbehandlingService
import java.time.Clock
import java.time.LocalDate

private const val PATH = "/sak/{sakId}/meldekort/{meldekortId}/oppdater"

fun Route.oppdaterMeldekortbehandlingRoute(
    oppdaterMeldekortbehandlingService: OppdaterMeldekortbehandlingService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - saksbehandler har oppdatert et meldekort under behandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                val correlationId = call.correlationId()

                val body = runCatching {
                    call.deserializeOppdaterMeldekortbehandlingDTO(
                        hentKjedeId = {
                            oppdaterMeldekortbehandlingService.hentKjedeIdForMeldekort(
                                sakId = sakId,
                                meldekortId = meldekortId,
                            )
                        },
                    )
                }.getOrElse {
                    call.respond400BadRequest(errorJson = ugyldigRequest())
                    return@withMeldekortId
                }

                oppdaterMeldekortbehandlingService.oppdaterMeldekort(
                    kommando = body.toDomain(
                        saksbehandler = saksbehandler,
                        meldekortId = meldekortId,
                        sakId = sakId,
                        correlationId = correlationId,
                    ),
                    clock = clock,
                ).fold(
                    ifLeft = {
                        respondWithError(it)
                    },
                    ifRight = {
                        auditService.logMedMeldekortId(
                            meldekortId = meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler har oppdatert et meldekort under behandling",
                            correlationId = correlationId,
                        )
                        call.respondJson(
                            value = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock),
                        )
                    },
                )
            }
        }
    }
}

suspend fun RoutingContext.respondWithError(meldekort: KanIkkeOppdatereMeldekortbehandling) {
    when (meldekort) {
        is MeldekortperiodenKanIkkeVæreFremITid -> {
            call.respond400BadRequest(
                melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
            )
        }
    }
}

// TODO: slett når frontend er oppdatert
private data class LegacyOppdaterMeldekortbehandlingDTO(
    val dager: List<LegacyOppdaterMeldekortdagDTO>,
    val begrunnelse: String?,
    val tekstTilVedtaksbrev: String?,
    val skalSendeVedtaksbrev: Boolean,
) {
    data class LegacyOppdaterMeldekortdagDTO(
        val dato: LocalDate,
        val status: MeldekortDagStatusDTO,
    )

    fun toNyDto(kjedeId: MeldeperiodeKjedeId): OppdaterMeldekortbehandlingDTO {
        return OppdaterMeldekortbehandlingDTO(
            meldeperioder = listOf(
                OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO(
                    kjedeId = kjedeId.verdi,
                    dager = dager.map {
                        OppdaterMeldekortbehandlingDTO.OppdaterMeldekortdagDTO(
                            dato = it.dato,
                            status = it.status,
                        )
                    },
                ),
            ),
            begrunnelse = begrunnelse,
            tekstTilVedtaksbrev = tekstTilVedtaksbrev,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        )
    }
}

private suspend fun RoutingCall.deserializeOppdaterMeldekortbehandlingDTO(
    hentKjedeId: () -> MeldeperiodeKjedeId,
): OppdaterMeldekortbehandlingDTO {
    return this.receiveText().let { body ->
        runCatching {
            deserialize<OppdaterMeldekortbehandlingDTO>(body)
        }.getOrElse {
            deserialize<LegacyOppdaterMeldekortbehandlingDTO>(body).toNyDto(hentKjedeId())
        }
    }
}
