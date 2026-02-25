package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ferdigstill

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
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.FerdigstillKlagebehandlingCommand
import no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill.KunneIkkeFerdigstilleKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.tilKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.service.FerdigstillKlagebehandlingService

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/ferdigstill"

fun Route.ferdigstillKlagebehandlingRoute(
    ferdigstillKlagebehandlingService: FerdigstillKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt put-request på '$PATH' - Ferdigstiller klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                ferdigstillKlagebehandlingService.ferdigstill(
                    command = FerdigstillKlagebehandlingCommand(
                        sakId = sakId,
                        klagebehandlingId = klagebehandlingId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ),
                ).fold(
                    ifLeft = {
                        call.respondJson(it.toStatusAndErrorJson())
                    },
                    ifRight = { klagebehandling ->
                        logger.info { "Ferdigstilt klagebehandling med id ${klagebehandling.id} for sak $sakId" }
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Oppdaterer brevtekst på klagebehandling på sak $sakId",
                            correlationId = correlationId,
                            behandlingId = klagebehandling.id,
                        )
                        call.respondJson(value = klagebehandling.tilKlagebehandlingDTO())
                    },
                )
            }
        }
    }
}

fun KunneIkkeFerdigstilleKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KunneIkkeFerdigstilleKlagebehandling.KreverUtfallFraKlageinstans -> TODO()

        KunneIkkeFerdigstilleKlagebehandling.ResultatMåVæreOpprettholdt -> TODO()

        is KunneIkkeFerdigstilleKlagebehandling.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(
                this.forventetSaksbehandler,
            ),
        )

        KunneIkkeFerdigstilleKlagebehandling.UtfallFraKlageinstansSkalFøreTilNyRammebehandling -> TODO()
    }
}
