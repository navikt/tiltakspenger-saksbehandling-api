package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withRammebehandlingId
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.infra.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter.KanIkkeKonvertereRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.KonverterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.StartRevurderingTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.KonverterRevurderingService
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.loggOgSvarFeil
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/konverter"

fun Route.konverterRevurderingRoute(
    konverterRevurderingService: KonverterRevurderingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}
    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Konverterer revurdering til en annen type" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post

        call.withSakId { sakId ->
            call.withRammebehandlingId { behandlingId ->
                call.withBody<KonverterRevurderingDTO> { body ->
                    if (body.revurderingType == StartRevurderingTypeDTO.FORLENGELSE && body.rammevedtakIdSomForlenges == null) {
                        call.respond400BadRequest(
                            melding = "rammevedtakIdSomForlenges må være satt for å konvertere til forlengelse.",
                            kode = "mangler_rammevedtak_som_forlenges",
                        )
                        return@withBody
                    }
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    konverterRevurderingService.konverter(
                        body.tilKommando(sakId, behandlingId, saksbehandler),
                    ).fold(
                        ifLeft = { feil ->
                            call.loggOgSvarFeil(
                                logger = logger,
                                operasjon = "Konvertere revurdering",
                                feil = feil,
                                statusOgErrorJson = feil.tilStatusOgErrorJson(),
                                kontekst = "sakId=$sakId, behandlingId=$behandlingId, correlationId=$correlationId",
                            )
                        },
                        ifRight = { (sak) ->
                            auditService.logMedRammebehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Konverterer revurderingen til ${body.revurderingType}",
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

fun KanIkkeKonvertereRevurdering.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    KanIkkeKonvertereRevurdering.ErIkkeRevurdering -> HttpStatusCode.BadRequest to ErrorJson(
        "Bare revurderinger kan konverteres.",
        "må_være_revurdering",
    )

    KanIkkeKonvertereRevurdering.MåVæreSaksbehandler -> HttpStatusCode.Forbidden to ErrorJson(
        "Du må ha saksbehandlerrollen for å konvertere revurderingen.",
        "må_være_saksbehandler",
    )

    is KanIkkeKonvertereRevurdering.MåVæreUnderBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Revurderingen må være under behandling for å kunne konverteres. Status er $status.",
        "må_være_under_behandling",
    )

    is KanIkkeKonvertereRevurdering.EiesAvAnnenSaksbehandler -> HttpStatusCode.BadRequest to Standardfeil.behandlingenEiesAvAnnenSaksbehandler(
        eiesAvSaksbehandler,
    )

    KanIkkeKonvertereRevurdering.ErSattPåVent -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er satt på vent.",
        "behandlingen_kan_ikke_være_satt_på_vent",
    )

    KanIkkeKonvertereRevurdering.HarKlagebehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Revurderingen er knyttet til en klagebehandling og kan ikke konverteres.",
        "har_klagebehandling",
    )

    KanIkkeKonvertereRevurdering.HarAlleredeTypen -> HttpStatusCode.BadRequest to ErrorJson(
        "Revurderingen er allerede av denne typen.",
        "har_allerede_typen",
    )

    is KanIkkeKonvertereRevurdering.VedtaketKanIkkeForlenges -> HttpStatusCode.BadRequest to ErrorJson(
        "Vedtaket kan ikke forlenges fordi det ikke er innvilget helt frem til slutten av vedtaksperioden, eller fordi dagen etter dekkes av et annet vedtak.",
        "vedtak_kan_ikke_forlenges",
    )

    KanIkkeKonvertereRevurdering.ForlengelseErIkkePåslått -> HttpStatusCode.BadRequest to ErrorJson(
        "Forlengelse er ikke tilgjengelig ennå.",
        "forlengelse_er_ikke_påslått",
    )
}
