package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.LeggTilbakeMeldekortbehandlingService

private const val LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/legg-tilbake"

fun Route.leggTilbakeMeldekortbehandlingRoute(
    auditService: AuditService,
    leggTilbakeMeldekortbehandlingService: LeggTilbakeMeldekortbehandlingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH' - Fjerner saksbehandler/beslutter fra meldekortbehandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                leggTilbakeMeldekortbehandlingService.leggTilbakeMeldekortbehandling(
                    sakId = sakId,
                    meldekortId = meldekortId,
                    saksbehandler = saksbehandler,
                ).also { (sak, behandling) ->
                    auditService.logMedMeldekortId(
                        meldekortId = meldekortId,
                        navIdent = saksbehandler.navIdent,
                        action = AuditLogEvent.Action.UPDATE,
                        contextMessage = "Saksbehandler fjernes fra meldekortbehandlingen",
                        correlationId = correlationId,
                    )

                    call.respondJson(
                        value = behandling.tilMeldekortbehandlingDTO(beregninger = sak.meldeperiodeBeregninger),
                    )
                }
            }
        }
    }
}
