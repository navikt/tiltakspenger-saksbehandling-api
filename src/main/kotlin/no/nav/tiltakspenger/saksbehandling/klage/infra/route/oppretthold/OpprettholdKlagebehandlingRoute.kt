package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppretthold

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.nå
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.KanIkkeOpprettholdeKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.tilKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettholdKlagebehandlingService
import java.time.Clock

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/oppretthold"

fun Route.opprettholdKlagebehandlingRoute(
    opprettholdKlagebehandlingService: OpprettholdKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Iverksetter opprettholdelse på klagebehandling - fører til innstillingsbrev + oversende av klage til klageinstans" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                opprettholdKlagebehandlingService.oppretthold(
                    kommando = OpprettholdKlagebehandlingKommando(
                        sakId = sakId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                        klagebehandlingId = klagebehandlingId,
                        tidspunkt = nå(clock),
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
                            contextMessage = "Iverksetter opprettholdelse på klagebehandling på sak $sakId",
                            correlationId = correlationId,
                            behandlingId = behandlingId,
                        )
                        call.respondJson(value = behandling.tilKlagebehandlingDTO())
                    },
                )
            }
        }
    }
}

private fun KanIkkeOpprettholdeKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeOpprettholdeKlagebehandling.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(
                this.forventetSaksbehandler,
            ),
        )

        is KanIkkeOpprettholdeKlagebehandling.FeilResultat -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan kun opprettholdef klagebehandling med resultat OPPRETTHOLDT",
                "må_ha_resultat_opprettholdt",
            ),
        )

        is KanIkkeOpprettholdeKlagebehandling.MåHaStatusUnderBehandling -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan kun opprettholde klagebehandling med status UNDER_BEHANDLING",
                "må_ha_status_under_behandling",
            ),
        )

        KanIkkeOpprettholdeKlagebehandling.ManglerBrevtekst -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Kan ikke opprettholde klagebehandling uten brevtekst",
                "mangler_brevtekst",
            ),
        )
    }
}
