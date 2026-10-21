package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withMeldekortId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdaterForBeslutter.KanIkkeOppdatereMeldekortbehandlingForBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdaterForBeslutter.OppdaterMeldekortbehandlingForBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortbehandlingForBeslutterService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/meldekort/{meldekortId}/beslutter/oppdater"

private data class OppdaterMeldekortbehandlingForBeslutterBody(val skalJournalføreNotat: Boolean)

fun Route.oppdaterMeldekortbehandlingForBeslutterRoute(
    oppdaterMeldekortbehandlingForBeslutterService: OppdaterMeldekortbehandlingForBeslutterService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    post(PATH) {
        logger.debug { "Mottatt post-request på $PATH - beslutter oppdaterer meldekortbehandlingen" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                call.withBody<OppdaterMeldekortbehandlingForBeslutterBody> { body ->
                    val correlationId = call.correlationId()
                    krevBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    oppdaterMeldekortbehandlingForBeslutterService.oppdaterForBeslutter(
                        OppdaterMeldekortbehandlingForBeslutterKommando(
                            sakId = sakId,
                            meldekortId = meldekortId,
                            skalJournalføreNotat = body.skalJournalføreNotat,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(statusAndValue = it.toErrorJson())
                        },
                        ifRight = { (sak) ->
                            auditService.logMedMeldekortId(
                                meldekortId = meldekortId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Beslutter oppdaterer meldekortbehandling $meldekortId",
                                correlationId = correlationId,
                            )
                            call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeOppdatereMeldekortbehandlingForBeslutter.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeOppdatereMeldekortbehandlingForBeslutter.BehandlingenErIkkeUnderBeslutning -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke under beslutning",
        "behandlingen_er_ikke_klar_til_beslutning",
    )

    KanIkkeOppdatereMeldekortbehandlingForBeslutter.MåVæreBeslutterForMeldekortet -> HttpStatusCode.BadRequest to ErrorJson(
        "Du kan ikke oppdatere meldekortbehandlingen da du ikke er beslutter for den",
        "må_være_beslutter_for_meldekortet",
    )

    KanIkkeOppdatereMeldekortbehandlingForBeslutter.BehandlingenErSattPåVent -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke oppdatere behandlingen fordi den er satt på vent",
        "behandlingen_kan_ikke_være_satt_på_vent",
    )

    KanIkkeOppdatereMeldekortbehandlingForBeslutter.MåHaBegrunnelseForÅJournalføreNotat -> HttpStatusCode.BadRequest to Standardfeil.måHaBegrunnelseForÅJournalføreNotat()

    KanIkkeOppdatereMeldekortbehandlingForBeslutter.BehandlingenErIkkeLengerUnderBeslutning -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Meldekortbehandlingen er ikke lenger sendt til beslutning.",
        kode = "må_være_sendt_til_beslutning",
    )
}
