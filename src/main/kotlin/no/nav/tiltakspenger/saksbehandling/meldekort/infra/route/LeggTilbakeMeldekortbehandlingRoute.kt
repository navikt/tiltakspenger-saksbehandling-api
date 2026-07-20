package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake.KanIkkeLeggeTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.LeggTilbakeMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/legg-tilbake"

fun Route.leggTilbakeMeldekortbehandlingRoute(
    auditService: AuditService,
    leggTilbakeMeldekortbehandlingService: LeggTilbakeMeldekortbehandlingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    post(LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$LEGG_TILBAKE_MELDEKORTBEHANDLING_PATH' - Fjerner saksbehandler/beslutter fra meldekortbehandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                leggTilbakeMeldekortbehandlingService.leggTilbakeMeldekortbehandling(
                    sakId = sakId,
                    meldekortId = meldekortId,
                    saksbehandler = saksbehandler,
                ).fold(
                    { call.respondJson(statusAndValue = it.tilStatusOgErrorJson()) },
                    { (sak) ->
                        auditService.logMedMeldekortId(
                            meldekortId = meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Saksbehandler fjernes fra meldekortbehandlingen",
                            correlationId = correlationId,
                        )

                        call.respondJson(
                            value = sak.toSakDTO(saksbehandler = saksbehandler, clock = clock),
                        )
                    },
                )
            }
        }
    }
}

fun KanIkkeLeggeTilbakeMeldekortbehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandler for å legge tilbake denne meldekortbehandlingen.",
        "maa_vaere_saksbehandler",
    )

    KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreSaksbehandlerForMeldekortet -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandleren som er tildelt meldekortbehandlingen for å legge den tilbake.",
        "maa_vaere_saksbehandler_for_meldekortet",
    )

    KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreBeslutter -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være beslutter for å legge tilbake denne meldekortbehandlingen.",
        "maa_vaere_beslutter",
    )

    KanIkkeLeggeTilbakeMeldekortbehandling.MåVæreBeslutterForMeldekortet -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være beslutteren som er tildelt meldekortbehandlingen for å legge den tilbake.",
        "maa_vaere_beslutter_for_meldekortet",
    )

    is KanIkkeLeggeTilbakeMeldekortbehandling.UgyldigStatus -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan ikke legge tilbake meldekortbehandling med status ${this.status}.",
        "ugyldig_status_for_legg_tilbake",
    )
}
