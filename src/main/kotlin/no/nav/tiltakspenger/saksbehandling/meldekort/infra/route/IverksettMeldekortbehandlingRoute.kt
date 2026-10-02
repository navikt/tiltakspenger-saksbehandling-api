package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.ErrorJsonBase
import no.nav.tiltakspenger.libs.ktor.common.ErrorJsonMedData
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.saksbehandlerOgBeslutterKanIkkeVæreLik
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.IverksettMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.KanIkkeIverksetteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.SakDTO
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilErrorJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes.tilSimuleringErrorJson
import java.time.Clock

private const val PATH = "sak/{sakId}/meldekort/{meldekortId}/iverksett"

fun Route.iverksettMeldekortRoute(
    iverksettMeldekortbehandlingService: IverksettMeldekortbehandlingService,
    auditService: AuditService,
    clock: Clock,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger { }

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - iverksetter meldekort" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                iverksettMeldekortbehandlingService.iverksettMeldekort(
                    IverksettMeldekortbehandlingKommando(
                        meldekortId = meldekortId,
                        beslutter = saksbehandler,
                        sakId = sakId,
                        correlationId = correlationId,
                    ),
                ).fold(
                    ifLeft = { feil ->
                        call.respondJson(statusAndValue = feil.statusOgErrorJson(saksbehandler, clock))
                    },
                    ifRight = { (sak) ->
                        auditService.logMedMeldekortId(
                            meldekortId = meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Iverksetter meldekort",
                            correlationId = correlationId,
                        )
                        call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                    },
                )
            }
        }
    }
}

/**
 * Returnerer [ErrorJsonBase] i stedet for [ErrorJson], fordi [KanIkkeIverksetteMeldekortbehandling.UtbetalingStøttesIkke] svarer med saken som data.
 */
fun KanIkkeIverksetteMeldekortbehandling.statusOgErrorJson(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Pair<HttpStatusCode, ErrorJsonBase> = when (this) {
    KanIkkeIverksetteMeldekortbehandling.SaksbehandlerOgBeslutterKanIkkeVæreLik -> HttpStatusCode.BadRequest to saksbehandlerOgBeslutterKanIkkeVæreLik()

    KanIkkeIverksetteMeldekortbehandling.BehandlingenErIkkeUnderBeslutning -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Du kan ikke godkjenne meldekort som ikke er under beslutning",
        kode = "meldekort_må_være_under_beslutning",
    )

    KanIkkeIverksetteMeldekortbehandling.BehandlingenErIkkeLengerUnderBeslutning -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Meldekortbehandlingen er ikke lenger under beslutning. Saksbehandler kan ha angret sendingen til beslutning.",
        kode = "behandlingen_er_ikke_under_beslutning",
    )

    KanIkkeIverksetteMeldekortbehandling.MåVæreBeslutterForMeldekortet -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Du kan ikke godkjenne meldekortet da du ikke er beslutter for denne meldekortbehandlingen",
        kode = "må_være_beslutter_for_meldekortet",
    )

    KanIkkeIverksetteMeldekortbehandling.MeldeperiodeneErIkkeSisteVersjon -> HttpStatusCode.BadRequest to ErrorJson(
        melding = "Meldeperiodene må være siste versjon for å kunne iverksette meldekortet",
        kode = "meldeperiodene_er_ikke_siste_versjon",
    )

    is KanIkkeIverksetteMeldekortbehandling.SimuleringFeil -> feil.tilSimuleringErrorJson()

    is KanIkkeIverksetteMeldekortbehandling.UtbetalingStøttesIkke -> tilErrorJsonMedSak(saksbehandler, clock)
}

private fun KanIkkeIverksetteMeldekortbehandling.UtbetalingStøttesIkke.tilErrorJsonMedSak(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Pair<HttpStatusCode, ErrorJsonMedData<SakDTO>> = this.feil.tilErrorJson().let { (status, errorJson) ->
    status to ErrorJsonMedData(
        melding = errorJson.melding,
        kode = errorJson.kode,
        data = this.sak.toSakDTO(saksbehandler, clock),
    )
}
