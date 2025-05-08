package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KunneIkkeHenteSakForSakId
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeSendeMeldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.SendMeldekortTilBeslutterDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutterService
import java.time.Clock

fun Route.sendMeldekortTilBeslutterRoute(
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutterService,
    auditService: AuditService,
    tokenService: TokenService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    post("/sak/{sakId}/meldekort/{meldekortId}") {
        logger.debug { "Mottatt post-request på /sak/{sakId}/meldekort/{meldekortId} - saksbehandler har fylt ut meldekortet og sendt til beslutter" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    call.withBody<SendMeldekortTilBeslutterDTO> { body ->
                        val correlationId = call.correlationId()
                        val kommando = body.toDomain(
                            saksbehandler = saksbehandler,
                            meldekortId = meldekortId,
                            sakId = sakId,
                            correlationId = correlationId,
                        )
                        sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(kommando).fold(
                            ifLeft = {
                                when (it) {
                                    is KanIkkeSendeMeldekortTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid -> {
                                        call.respond400BadRequest(
                                            melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                                            kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
                                        )
                                    }

                                    is KanIkkeSendeMeldekortTilBeslutter.MåVæreSaksbehandler -> {
                                        call.respond400BadRequest(
                                            melding = "Kan ikke sende meldekort til beslutter. Krever saksbehandler-rolle.",
                                            kode = "må_være_saksbehandler",
                                        )
                                    }

                                    is KanIkkeSendeMeldekortTilBeslutter.MåVæreSaksbehandlerForMeldekortet -> {
                                        call.respond400BadRequest(
                                            melding = "Du kan ikke sende meldekortet til beslutter da du ikke er saksbehandler for denne meldekortbehandlingen",
                                            kode = "må_være_saksbehandler_for_meldekortet",
                                        )
                                    }

                                    is KanIkkeSendeMeldekortTilBeslutter.KunneIkkeHenteSak -> when (
                                        val u =
                                            it.underliggende
                                    ) {
                                        is KunneIkkeHenteSakForSakId.HarIkkeTilgang -> call.respond403Forbidden(
                                            Standardfeil.ikkeTilgang("Må ha en av rollene ${u.kreverEnAvRollene} for å hente sak"),
                                        )
                                    }

                                    is KanIkkeSendeMeldekortTilBeslutter.KanIkkeOppdatere -> respondWithError(it.underliggende)
                                }
                            },
                            ifRight = {
                                auditService.logMedMeldekortId(
                                    meldekortId = meldekortId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Saksbehandler har fylt ut meldekortet og sendt til beslutter",
                                    correlationId = correlationId,
                                )
                                call.respond(
                                    message = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock),
                                    status = HttpStatusCode.OK,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
