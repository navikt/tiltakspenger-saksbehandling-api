package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
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
import no.nav.tiltakspenger.saksbehandling.infra.repo.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.kanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingFormkravService

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/formkrav"

fun Route.oppdaterKlagebehandlingFormkravRoute(
    oppdaterKlagebehandlingFormkravService: OppdaterKlagebehandlingFormkravService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    put(PATH) {
        logger.debug { "Mottatt put-request på '$PATH' - Oppdaterer formkrav på eksisterende klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@put
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@put
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<OppdaterKlagebehandlingFormkravBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    oppdaterKlagebehandlingFormkravService
                        .oppdaterFormkrav(
                            kommando =
                            body.tilKommando(
                                sakId = sakId,
                                saksbehandler = saksbehandler,
                                correlationId = correlationId,
                                klagebehandlingId = klagebehandlingId,
                            ),
                        ).fold(
                            ifLeft = {
                                call.respondJson(it.toStatusAndErrorJson())
                            },
                            ifRight = { (_, behandling) ->
                                val behandlingId = behandling.id
                                auditService.logMedSakId(
                                    sakId = sakId,
                                    navIdent = saksbehandler.navIdent,
                                    action = AuditLogEvent.Action.UPDATE,
                                    contextMessage = "Oppdaterer formkrav på klagebehandling på sak $sakId",
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
}

fun KanIkkeOppdatereKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeOppdatereKlagebehandling.FantIkkeJournalpost -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Fant ikke journalpost",
                    "fant_ikke_journalpost",
                ),
            )
        }

        is KanIkkeOppdatereKlagebehandling.SaksbehandlerMismatch -> {
            Pair(
                HttpStatusCode.BadRequest,
                behandlingenEiesAvAnnenSaksbehandler(
                    this.forventetSaksbehandler,
                ),
            )
        }

        is KanIkkeOppdatereKlagebehandling.KanIkkeOppdateres -> {
            Pair(
                HttpStatusCode.BadRequest,
                kanIkkeOppdatereBehandling(),
            )
        }
    }
}
