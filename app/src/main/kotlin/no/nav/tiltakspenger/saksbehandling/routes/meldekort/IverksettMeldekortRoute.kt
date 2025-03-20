package no.nav.tiltakspenger.saksbehandling.routes.meldekort

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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort.MåVæreBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort.SaksbehandlerOgBeslutterKanIkkeVæreLik
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.saksbehandling.routes.correlationId
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.måVæreBeslutter
import no.nav.tiltakspenger.saksbehandling.routes.exceptionhandling.Standardfeil.saksbehandlerOgBeslutterKanIkkeVæreLik
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto.toDTO
import no.nav.tiltakspenger.saksbehandling.routes.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.routes.withSakId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.KunneIkkeHenteSakForSakId

fun Route.iverksettMeldekortRoute(
    iverksettMeldekortService: IverksettMeldekortService,
    auditService: AuditService,
    tokenService: TokenService,
) {
    val logger = KotlinLogging.logger { }

    post("sak/{sakId}/meldekort/{meldekortId}/iverksett") {
        logger.debug { "Mottatt post-request på sak/{sakId}/meldekort/{meldekortId}/iverksett - iverksetter meldekort" }
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withSakId { sakId ->
                call.withMeldekortId { meldekortId ->
                    val correlationId = call.correlationId()
                    val meldekort = iverksettMeldekortService.iverksettMeldekort(
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
                    meldekort.fold(
                        {
                            when (it) {
                                is MåVæreBeslutter -> call.respond403Forbidden(måVæreBeslutter())
                                is SaksbehandlerOgBeslutterKanIkkeVæreLik -> call.respond400BadRequest(
                                    saksbehandlerOgBeslutterKanIkkeVæreLik(),
                                )

                                is KanIkkeIverksetteMeldekort.KunneIkkeHenteSak -> when (val u = it.underliggende) {
                                    is KunneIkkeHenteSakForSakId.HarIkkeTilgang -> call.respond403Forbidden(
                                        ikkeTilgang("Må ha en av rollene ${u.kreverEnAvRollene} for å hente sak"),
                                    )
                                }
                            }
                        },
                        { call.respond(HttpStatusCode.OK, it.toDTO()) },
                    )
                }
            }
        }
    }
}
