package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeSendeMeldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilErrorJson
import java.time.Clock

fun Route.sendMeldekortTilBeslutterRoute(
    sendMeldekortTilBeslutterService: SendMeldekortTilBeslutterService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }
    post("/sak/{sakId}/meldekort/{meldekortId}") {
        logger.debug { "Mottatt post-request på /sak/{sakId}/meldekort/{meldekortId} - saksbehandler har fylt ut meldekortet og sendt til beslutter" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortTilBeslutterKommando(
                        saksbehandler = saksbehandler,
                        meldekortId = meldekortId,
                        sakId = sakId,
                        correlationId = correlationId,
                    ),
                ).fold(
                    ifLeft = {
                        when (it) {
                            is KanIkkeSendeMeldekortTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid -> {
                                call.respond400BadRequest(
                                    melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                                    kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
                                )
                            }

                            is KanIkkeSendeMeldekortTilBeslutter.KanIkkeOppdatere -> respondWithError(it.underliggende)

                            is KanIkkeSendeMeldekortTilBeslutter.UtbetalingStøttesIkke -> call.respondJson(valueAndStatus = it.feil.tilErrorJson())
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
                        call.respondJson(
                            value = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock),
                        )
                    },
                )
            }
        }
    }
}
