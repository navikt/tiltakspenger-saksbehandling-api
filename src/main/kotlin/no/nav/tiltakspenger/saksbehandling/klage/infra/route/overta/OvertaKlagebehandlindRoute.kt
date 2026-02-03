package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.KanIkkeOvertaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.OvertaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.toStatusAndErrorJson
import no.nav.tiltakspenger.saksbehandling.klage.service.OvertaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private data class OvertaKlagebehandlingBody(
    val overtarFra: String,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        klagebehandlingId: KlagebehandlingId,
        correlationId: CorrelationId,
    ): OvertaKlagebehandlingKommando {
        return OvertaKlagebehandlingKommando(
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            saksbehandler = saksbehandler,
            overtarFra = overtarFra,
            correlationId = correlationId,
        )
    }
}

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/overta"

fun Route.overtaKlagebehandlingRoute(
    overtaKlagebehandlingService: OvertaKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Overta klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<OvertaKlagebehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    overtaKlagebehandlingService.overta(
                        kommando = body.tilKommando(
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
                                contextMessage = "Overtar klagebehandling på sak $sakId",
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
}

/**
 * Brukes også av overta fra rammebehandling
 */
fun KanIkkeOvertaKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeOvertaKlagebehandling.KanIkkeOppdateres -> {
            this.underliggende.toStatusAndErrorJson()
        }

        is KanIkkeOvertaKlagebehandling.BrukTaKlagebehandlingIsteden -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan ikke overta klagebehandling. Bruk ta klagebehandling isteden.",
                "kan_ikke_overta_klagebehandling_bruk_ta_klagebehandling_isteden",
            ),
        )
    }
}
