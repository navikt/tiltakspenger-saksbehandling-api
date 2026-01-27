package no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev

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
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.kanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.KanIkkeIverksetteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.OppdaterKlagebehandlingFormkravBody
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.toDto
import no.nav.tiltakspenger.saksbehandling.klage.service.IverksettKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingFormkravService

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/iverksett"

fun Route.iverksettKlagebehandlingRoute(
    iverksettKlagebehandlingService: IverksettKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Iverksetter klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                iverksettKlagebehandlingService.iverksett(
                    kommando = IverksettKlagebehandlingKommando(
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                        klagebehandlingId = klagebehandlingId,
                    ),
                ).fold(
                    ifLeft = {
                        call.respondJson(it.toStatusAndErrorJson())
                    },
                    ifRight = { (_, vedtak) ->
                        val behandlingId = vedtak.behandling.id
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Iverksetter klagebehandling på sak $sakId",
                            correlationId = correlationId,
                            behandlingId = behandlingId,
                        )
                        call.respondJson(value = vedtak.behandling.toDto())
                    },
                )
            }
        }
    }
}

private fun KanIkkeIverksetteKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeIverksetteKlagebehandling.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(
                this.forventetSaksbehandler,
            ),
        )

        is KanIkkeIverksetteKlagebehandling.AndreGrunner -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan ikke iverksette behandling på grunn av: ${this.årsak.joinToString(", ")}",
                "kan_ikke_iverksette_behandling",
            ),
        )
        is KanIkkeIverksetteKlagebehandling.MåHaResultatAvvisning -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan kun iverksette klagebehandling med resultat AVVIST",
                "må_ha_resultat_avvisning",
            ),
        )
        is KanIkkeIverksetteKlagebehandling.MåHaStatusUnderBehandling -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan kun iverksette klagebehandling med status UNDER_BEHANDLING",
                "må_ha_status_under_behandling",
            ),
        )
    }
}

fun KanIkkeOppdatereKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeOppdatereKlagebehandling.FantIkkeJournalpost -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Fant ikke journalpost",
                "fant_ikke_journalpost",
            ),
        )

        is KanIkkeOppdatereKlagebehandling.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(
                this.forventetSaksbehandler,
            ),
        )

        is KanIkkeOppdatereKlagebehandling.KanIkkeOppdateres -> Pair(
            HttpStatusCode.BadRequest,
            kanIkkeOppdatereBehandling(),
        )
    }
}
