package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
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
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort.MeldekortperiodenKanIkkeVæreFremITid
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeKjedeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortService
import java.time.Clock

private const val PATH = "/sak/{sakId}/meldekort/{meldekortId}/oppdater"

fun Route.oppdaterMeldekortBehandlingRoute(
    oppdaterMeldekortService: OppdaterMeldekortService,
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
                call.withBody<OppdaterMeldekortDTO> { body ->
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    val correlationId = call.correlationId()
                    val kommando = body.toDomain(
                        saksbehandler = saksbehandler,
                        meldekortId = meldekortId,
                        sakId = sakId,
                        correlationId = correlationId,
                    )
                    oppdaterMeldekortService.oppdaterMeldekort(kommando).fold(
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
}

suspend fun RoutingContext.respondWithError(meldekort: KanIkkeOppdatereMeldekort) {
    when (meldekort) {
        is MeldekortperiodenKanIkkeVæreFremITid -> {
            call.respond400BadRequest(
                melding = "Kan ikke sende inn et meldekort før meldekortperioden har begynt.",
                kode = "meldekortperioden_kan_ikke_være_frem_i_tid",
            )
        }
    }
}
