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
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.saksbehandlerOgBeslutterKanIkkeVæreLik
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.IverksettMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.KanIkkeIverksetteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.KanIkkeIverksetteMeldekortbehandling.SaksbehandlerOgBeslutterKanIkkeVæreLik
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortbehandlingService
import java.time.Clock

fun Route.iverksettMeldekortRoute(
    iverksettMeldekortbehandlingService: IverksettMeldekortbehandlingService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }

    post("sak/{sakId}/meldekort/{meldekortId}/iverksett") {
        logger.debug { "Mottatt post-request på sak/{sakId}/meldekort/{meldekortId}/iverksett - iverksetter meldekort" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                iverksettMeldekortbehandlingService.iverksettMeldekort(
                    IverksettMeldekortbehandlingKommando(
                        meldekortId = meldekortId,
                        beslutter = saksbehandler,
                        sakId = sakId,
                        correlationId = correlationId,
                    ),
                ).fold(
                    {
                        when (it) {
                            is SaksbehandlerOgBeslutterKanIkkeVæreLik -> call.respond400BadRequest(
                                saksbehandlerOgBeslutterKanIkkeVæreLik(),
                            )

                            KanIkkeIverksetteMeldekortbehandling.BehandlingenErIkkeUnderBeslutning -> call.respond400BadRequest(
                                melding = "Du kan ikke godkjenne meldekort som ikke er under beslutning",
                                kode = "meldekort_må_være_under_beslutning",
                            )

                            KanIkkeIverksetteMeldekortbehandling.MåVæreBeslutterForMeldekortet -> call.respond400BadRequest(
                                melding = "Du kan ikke godkjenne meldekortet da du ikke er beslutter for denne meldekortbehandlingen",
                                kode = "må_være_beslutter_for_meldekortet",
                            )
                        }
                    },
                    {
                        auditService.logMedMeldekortId(
                            meldekortId = meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Iverksetter meldekort",
                            correlationId = correlationId,
                        )
                        call.respondJson(value = it.first.toMeldeperiodeKjedeDTO(it.second.kjedeId, clock))
                    },
                )
            }
        }
    }
}
