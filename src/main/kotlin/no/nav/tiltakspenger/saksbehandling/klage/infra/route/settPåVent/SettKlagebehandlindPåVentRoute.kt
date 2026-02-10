package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
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
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.KanIkkeSetteKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.toStatusAndErrorJson
import no.nav.tiltakspenger.saksbehandling.klage.service.SettKlagebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock
import java.time.LocalDate

private data class SettKlagebehandlingPåVentBody(
    val begrunnelse: String,
    val frist: LocalDate?,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        klagebehandlingId: KlagebehandlingId,
    ): SettKlagebehandlingPåVentKommando {
        return SettKlagebehandlingPåVentKommando(
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            frist = frist,
        )
    }
}

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/vent"

fun Route.settKlagebehandlingPåVentRoute(
    settKlagebehandlingPåVentService: SettKlagebehandlingPåVentService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Sett klagebehandling på vent" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<SettKlagebehandlingPåVentBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    settKlagebehandlingPåVentService.settPåVent(
                        kommando = body.tilKommando(
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
fun KanIkkeSetteKlagebehandlingPåVent.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeSetteKlagebehandlingPåVent.KanIkkeOppdateres -> {
            this.underliggende.toStatusAndErrorJson()
        }

        is KanIkkeSetteKlagebehandlingPåVent.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(
                this.forventetSaksbehandler,
            ),
        )
    }
}
