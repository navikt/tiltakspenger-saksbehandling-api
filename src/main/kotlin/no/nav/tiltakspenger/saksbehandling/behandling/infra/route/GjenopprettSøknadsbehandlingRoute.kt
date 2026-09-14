package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenopprett.GjenopprettSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenopprett.KanIkkeGjenoppretteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopprettSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil

private const val GJENOPPRETT_SØKNADSBEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/gjenopprett"

/**
 * Tar opp igjen en søknad som ble avsluttet uten vedtak.
 * Saksbehandler peker på den avbrutte søknadsbehandlingen, og får en ny søknadsbehandling tilbake.
 */
fun Route.gjenopprettSøknadsbehandlingRoute(
    gjenopprettSøknadsbehandlingService: GjenopprettSøknadsbehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(GJENOPPRETT_SØKNADSBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$GJENOPPRETT_SØKNADSBEHANDLING_PATH' - Gjenoppretter søknaden og oppretter en ny søknadsbehandling." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                call.withBody<GjenopprettSøknadsbehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    gjenopprettSøknadsbehandlingService.gjenopprettSøknadsbehandling(
                        GjenopprettSøknadsbehandlingKommando(
                            sakId = sakId,
                            avbruttBehandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            begrunnelse = body.begrunnelse?.toNonBlankString(),
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = { feil ->
                            call.loggOgSvarFeil(
                                logger = logger,
                                operasjon = "Gjenopprett søknadsbehandling",
                                feil = feil,
                                statusOgErrorJson = feil.tilStatusOgErrorJson(),
                                kontekst = "sakId=$sakId, behandlingId=$behandlingId",
                            )
                        },
                        ifRight = { (sak, nySøknadsbehandling) ->
                            auditService.logMedRammebehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.CREATE,
                                contextMessage = "Gjenoppretter søknaden og oppretter en ny søknadsbehandling",
                                correlationId = correlationId,
                            )
                            call.respondJson(
                                value = nySøknadsbehandling.tilSøknadsbehandlingDTO(
                                    utbetalingsstatus = null,
                                    beregninger = sak.meldeperiodeBeregninger,
                                    rammevedtakId = null,
                                    tilbakekrevingId = null,
                                    kallendeSaksbehandler = saksbehandler,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

/** Begrunnelsen er valgfri; gjenopprettingen forklares som regel av at det opprettes en ny behandling. */
data class GjenopprettSøknadsbehandlingBody(
    val begrunnelse: String?,
)

private fun KanIkkeGjenoppretteSøknadsbehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeGjenoppretteSøknadsbehandling.FantIkkeBehandling -> HttpStatusCode.NotFound to ErrorJson(
        "Behandlingen finnes ikke lenger.",
        "fant_ikke_behandling",
    )

    KanIkkeGjenoppretteSøknadsbehandling.BehandlingenErIkkeEnSøknadsbehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Det er bare søknadsbehandlinger som kan gjenopprettes.",
        "behandlingen_er_ikke_en_soknadsbehandling",
    )

    is KanIkkeGjenoppretteSøknadsbehandling.BehandlingenErIkkeAvbrutt -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke avbrutt, og kan derfor ikke gjenopprettes.",
        "behandlingen_er_ikke_avbrutt",
    )

    KanIkkeGjenoppretteSøknadsbehandling.SøknadenHarEnAktivBehandling -> HttpStatusCode.Conflict to ErrorJson(
        "Søknaden har allerede en behandling som ikke er avbrutt.",
        "soknaden_har_en_aktiv_behandling",
    )

    KanIkkeGjenoppretteSøknadsbehandling.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandler for å gjenopprette søknaden.",
        "maa_vaere_saksbehandler",
    )
}
