package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.GjenopptaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.KanIkkeGjenopptaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.GjenopptaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val GJENOPPTA_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/gjenoppta"

fun Route.gjenopptaMeldekortbehandlingRoute(
    auditService: AuditService,
    gjenopptaMeldekortbehandlingService: GjenopptaMeldekortbehandlingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    patch(GJENOPPTA_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt patch-request på '$GJENOPPTA_MELDEKORTBEHANDLING_PATH' - gjenopptar meldekortbehandling." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                gjenopptaMeldekortbehandlingService.gjenoppta(
                    kommando = GjenopptaMeldekortbehandlingKommando(
                        sakId = sakId,
                        meldekortId = meldekortId,
                        saksbehandler = saksbehandler,
                        correlationId = correlationId,
                    ),
                ).fold(
                    { call.respondGjenopptaError(it) },
                    { (sak) ->
                        auditService.logMedMeldekortId(
                            meldekortId = meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Meldekortbehandling er blitt gjenopptatt",
                            correlationId = correlationId,
                        )

                        call.respondJson(
                            value = sak.toSakDTO(saksbehandler, clock),
                        )
                    },
                )
            }
        }
    }
}

private suspend fun ApplicationCall.respondGjenopptaError(
    feil: KanIkkeGjenopptaMeldekortbehandling,
) {
    when (feil) {
        is KanIkkeGjenopptaMeldekortbehandling.BehandlingenErIkkePåVent -> respond400BadRequest(
            melding = "Meldekortbehandlingen er ikke satt på vent, og kan derfor ikke gjenopptas.",
            kode = "behandlingen_er_ikke_paa_vent",
        )

        is KanIkkeGjenopptaMeldekortbehandling.MåVæreSaksbehandler -> respond403Forbidden(
            melding = "Du må være saksbehandler for å gjenoppta denne meldekortbehandlingen.",
            kode = "maa_vaere_saksbehandler",
        )

        is KanIkkeGjenopptaMeldekortbehandling.MåVæreSaksbehandlerSomEierBehandlingen -> respond403Forbidden(
            melding = "Du må være saksbehandleren som er tildelt meldekortbehandlingen for å gjenoppta den.",
            kode = "maa_vaere_saksbehandler_som_eier_behandlingen",
        )

        is KanIkkeGjenopptaMeldekortbehandling.MåVæreBeslutter -> respond403Forbidden(
            melding = "Du må være beslutter for å gjenoppta denne meldekortbehandlingen.",
            kode = "maa_vaere_beslutter",
        )

        is KanIkkeGjenopptaMeldekortbehandling.BeslutterKanIkkeVæreSammeSomSaksbehandler -> respond400BadRequest(
            melding = "Beslutter kan ikke være den samme som saksbehandleren på meldekortbehandlingen.",
            kode = "beslutter_kan_ikke_vaere_samme_som_saksbehandler",
        )

        is KanIkkeGjenopptaMeldekortbehandling.UgyldigStatus -> respond400BadRequest(
            melding = "Kan ikke gjenoppta meldekortbehandling med status ${feil.status}.",
            kode = "ugyldig_status_for_gjenoppta",
        )
    }
}
