package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.KanIkkeSendeMeldekortbehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortbehandlingTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilErrorJson
import java.time.Clock

fun Route.sendMeldekortTilBeslutterRoute(
    sendMeldekortbehandlingTilBeslutterService: SendMeldekortbehandlingTilBeslutterService,
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
                sendMeldekortbehandlingTilBeslutterService.sendMeldekortTilBeslutter(
                    SendMeldekortbehandlingTilBeslutterKommando(
                        saksbehandler = saksbehandler,
                        meldekortId = meldekortId,
                        sakId = sakId,
                        correlationId = correlationId,
                    ),
                    clock = clock,
                ).fold(
                    ifLeft = {
                        when (it) {
                            is KanIkkeSendeMeldekortbehandlingTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid -> {
                                call.respond400BadRequest(
                                    melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                                    kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
                                )
                            }

                            is KanIkkeSendeMeldekortbehandlingTilBeslutter.KanIkkeOppdatere -> respondWithError(it.underliggende)

                            is KanIkkeSendeMeldekortbehandlingTilBeslutter.UtbetalingStøttesIkke -> call.respondJson(
                                statusAndValue = it.feil.tilErrorJson(),
                            )
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
                            value = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeIdLegacy, clock),
                        )
                    },
                )
            }
        }
    }
}
