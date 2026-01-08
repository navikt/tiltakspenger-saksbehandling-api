package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJsonString
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettKlagebehandlingService

private const val PATH = "/sak/{sakId}/klage"

fun Route.opprettKlagebehandlingRoute(
    opprettKlagebehandlingService: OpprettKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Oppretter en ny klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBody<OpprettKlagebehandlingBody> { body ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                opprettKlagebehandlingService.opprettKlagebehandling(
                    kommando = body.tilKommando(
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ),
                ).fold(
                    ifLeft = {
                        call.respondJsonString(status = HttpStatusCode.InternalServerError, json = """{"todo":"todo"}""")
                    },
                    ifRight = { (_, behandling) ->
                        val behandlingId = behandling.id
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.CREATE,
                            contextMessage = "Oppretter klagebehandling på sak $sakId",
                            correlationId = correlationId,
                            behandlingId = behandlingId,
                        )
                        call.respondJson(value = behandling.toDto())
                    },
                )
            }
        }
    }
}
