package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.infra.repo.Standardfeil.saksbehandlerOgBeslutterKanIkkeVæreLik
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort.SaksbehandlerOgBeslutterKanIkkeVæreLik
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import java.time.Clock

fun Route.iverksettMeldekortRoute(
    iverksettMeldekortService: IverksettMeldekortService,
    auditService: AuditService,
    tokenService: TokenService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    post("sak/{sakId}/meldekort/{meldekortId}/iverksett") {
        logger.debug { "Mottatt post-request på sak/{sakId}/meldekort/{meldekortId}/iverksett - iverksetter meldekort" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    val correlationId = call.correlationId()
                    val utbetalingsvedtak = iverksettMeldekortService.iverksettMeldekort(
                        IverksettMeldekortKommando(
                            meldekortId = meldekortId,
                            beslutter = saksbehandler,
                            sakId = sakId,
                            correlationId = correlationId,
                        ),
                    )
                    auditService.logMedMeldekortId(
                        meldekortId = meldekortId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = "Iverksetter meldekort",
                        correlationId = correlationId,
                    )
                    utbetalingsvedtak.fold(
                        {
                            when (it) {
                                is SaksbehandlerOgBeslutterKanIkkeVæreLik -> call.respond400BadRequest(
                                    saksbehandlerOgBeslutterKanIkkeVæreLik(),
                                )

                                KanIkkeIverksetteMeldekort.BehandlingenErIkkeUnderBeslutning -> call.respond400BadRequest(
                                    melding = "Du kan ikke godkjenne meldekort som ikke er under beslutning",
                                    kode = "meldekort_må_være_under_beslutning",
                                )

                                KanIkkeIverksetteMeldekort.MåVæreBeslutterForMeldekortet -> call.respond400BadRequest(
                                    melding = "Du kan ikke godkjenne meldekortet da du ikke er beslutter for denne meldekortbehandlingen",
                                    kode = "må_være_beslutter_for_meldekortet",
                                )
                            }
                        },
                        { call.respond(HttpStatusCode.OK, it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock)) },
                    )
                }
            }
        }
    }
}
