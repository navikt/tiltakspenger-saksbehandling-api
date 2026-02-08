package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
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
import no.nav.tiltakspenger.saksbehandling.infra.repo.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.GjenopptaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.KanIkkeGjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.toStatusAndErrorJson
import no.nav.tiltakspenger.saksbehandling.klage.service.GjenopptaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/gjenoppta"

fun Route.gjenopptaKlagebehandlingRoute(
    gjenopptaKlagebehandlingService: GjenopptaKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Gjenoppta klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                gjenopptaKlagebehandlingService.gjenoppta(
                    kommando = GjenopptaKlagebehandlingKommando(
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        klagebehandlingId = klagebehandlingId,
                        correlationId = correlationId,
                    ),
                ).fold(
                    ifLeft = {
                        call.respondJson(it.toStatusAndErrorJson())
                    },
                    ifRight = { (sak, behandling) ->
                        val behandlingId = behandling.id
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Gjenopptar klagebehandling på sak $sakId",
                            correlationId = correlationId,
                            behandlingId = behandlingId,
                        )
                        call.respondJson(value = sak.toSakDTO(clock))
                    },
                )
            }
        }
    }
}

/**
 * Brukes også av legg tilbake fra rammebehandling (omgjøring etter klage)
 */
fun KanIkkeGjenopptaKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeGjenopptaKlagebehandling.KanIkkeOppdateres -> {
            this.underliggende.toStatusAndErrorJson()
        }

        KanIkkeGjenopptaKlagebehandling.MåVæreSattPåVent -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Klagebehandlingen må være satt på vent for å kunne gjenopptas.",
                "klagebehandling_må_være_satt_på_vent",
            ),
        )
    }
}
