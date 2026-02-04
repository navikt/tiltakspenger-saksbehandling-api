package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

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
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.KanIkkeTaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.TaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.toStatusAndErrorJson
import no.nav.tiltakspenger.saksbehandling.klage.service.TaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/ta"

fun Route.taKlagebehandlingRoute(
    taKlagebehandlingService: TaKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Ta klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                taKlagebehandlingService.ta(
                    kommando = TaKlagebehandlingKommando(
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        klagebehandlingId = klagebehandlingId,
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
                            contextMessage = "Tar klagebehandling på sak $sakId",
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
 * Brukes også av ta fra rammebehandling
 */
fun KanIkkeTaKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeTaKlagebehandling.KanIkkeOppdateres -> {
            this.underliggende.toStatusAndErrorJson()
        }

        is KanIkkeTaKlagebehandling.BrukOvertaIsteden -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan ikke ta klagebehandling. Bruk overta klagebehandling isteden.",
                "kan_ikke_ta_klagebehandling_bruk_overta_klagebehandling_isteden",
            ),
        )
    }
}
