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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenåpne.GjenåpneSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenåpne.KanIkkeGjenåpneSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenåpneSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil

private const val GJENÅPNE_SØKNADSBEHANDLING_PATH = "/sak/{sakId}/behandling/{behandlingId}/gjenapne"

fun Route.gjenåpneSøknadsbehandlingRoute(
    gjenåpneSøknadsbehandlingService: GjenåpneSøknadsbehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(GJENÅPNE_SØKNADSBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$GJENÅPNE_SØKNADSBEHANDLING_PATH' - Gjenåpner søknaden og oppretter en ny søknadsbehandling." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                call.withBody<GjenåpneSøknadsbehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    gjenåpneSøknadsbehandlingService.gjenåpneSøknadsbehandling(
                        GjenåpneSøknadsbehandlingKommando(
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
                                operasjon = "Gjenåpne søknadsbehandling",
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
                                contextMessage = "Gjenåpner søknaden og oppretter en ny søknadsbehandling",
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

/** Begrunnelsen er valgfri; gjenåpningen forklares som regel av at det opprettes en ny behandling. */
data class GjenåpneSøknadsbehandlingBody(
    val begrunnelse: String?,
)

private fun KanIkkeGjenåpneSøknadsbehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeGjenåpneSøknadsbehandling.FantIkkeBehandling -> HttpStatusCode.NotFound to ErrorJson(
        "Behandlingen finnes ikke lenger.",
        "fant_ikke_behandling",
    )

    KanIkkeGjenåpneSøknadsbehandling.BehandlingenErIkkeEnSøknadsbehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Det er bare søknadsbehandlinger som kan gjenåpnes.",
        "behandlingen_er_ikke_en_soknadsbehandling",
    )

    is KanIkkeGjenåpneSøknadsbehandling.BehandlingenErIkkeAvbrutt -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke avbrutt, og kan derfor ikke gjenåpnes.",
        "behandlingen_er_ikke_avbrutt",
    )

    KanIkkeGjenåpneSøknadsbehandling.SøknadenHarEnAktivBehandling -> HttpStatusCode.Conflict to ErrorJson(
        "Søknaden har allerede en behandling som ikke er avbrutt.",
        "soknaden_har_en_aktiv_behandling",
    )

    KanIkkeGjenåpneSøknadsbehandling.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må være saksbehandler for å gjenåpne søknaden.",
        "maa_vaere_saksbehandler",
    )
}
