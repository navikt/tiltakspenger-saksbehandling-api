package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
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
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.OvertaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OvertaMeldekortbehandlingService

internal const val OVERTA_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/overta"

data class OvertaBehandlingBody(
    val overtarFra: String,
)

fun Route.overtaMeldekortbehandlingRoute(
    overtaMeldekortbehandlingService: OvertaMeldekortbehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    patch(OVERTA_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$OVERTA_MELDEKORTBEHANDLING_PATH' - Tar over meldekortbehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                call.withBody<OvertaBehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    overtaMeldekortbehandlingService.overta(
                        OvertaMeldekortbehandlingKommando(
                            sakId = sakId,
                            meldekortId = meldekortId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            overtarFra = body.overtarFra,
                        ),
                    ).fold(
                        {
                            call.respondJson(statusAndValue = it.tilStatusOgErrorJson())
                        },
                        { (sak, behandling) ->
                            auditService.logMedMeldekortId(
                                meldekortId = meldekortId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Overtar meldekortbehandlingen",
                                correlationId = correlationId,
                            )

                            call.respondJson(
                                value = behandling.tilMeldekortbehandlingDTO(beregninger = sak.meldeperiodeBeregninger),
                            )
                        },
                    )
                }
            }
        }
    }
}

internal fun KunneIkkeOvertaMeldekortbehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KunneIkkeOvertaMeldekortbehandling.BehandlingenKanIkkeVæreGodkjentEllerIkkeRett -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen kan ikke være godkjent eller ikke rett til tiltakspenger",
            "behandlingen_kan_ikke_være_godkjent_eller_ikke_rett",
        )

        KunneIkkeOvertaMeldekortbehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen har ikke en eksisterende saksbehandler å overta fra",
            "behandlingen_har_ikke_eksisterende_saksbehandler",
        )

        KunneIkkeOvertaMeldekortbehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen har ikke en eksisterende beslutter å overta fra",
            "behandlingen_har_ikke_eksisterende_beslutter",
        )

        KunneIkkeOvertaMeldekortbehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme -> HttpStatusCode.BadRequest to ErrorJson(
            "Saksbehandler og beslutter kan ikke være den samme på samme behandling",
            "saksbehandler_og_beslutter_kan_ikke_vær_den_samme",
        )

        KunneIkkeOvertaMeldekortbehandling.BehandlingenMåVæreUnderBeslutningForÅOverta -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen må være under beslutning for å overta",
            "behandling_må_være_under_beslutning",
        )

        KunneIkkeOvertaMeldekortbehandling.KanIkkeOvertaAutomatiskBehandling -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandlingen kan ikke være en automatisk behandling",
            "behandling_kan_ikke_være_automatisk",
        )
    }
}
