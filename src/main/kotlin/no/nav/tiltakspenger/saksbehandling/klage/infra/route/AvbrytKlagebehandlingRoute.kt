package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.NonBlankString
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.service.AvbrytKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/avbryt"

data class AvbrytKlagebehandlingBody(
    val begrunnelse: String,
)

fun Route.avbrytKlagebehandlingRoute(
    avbrytKlagebehandlingService: AvbrytKlagebehandlingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - Avbryter klagebehandling" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<AvbrytKlagebehandlingBody> {
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    avbrytKlagebehandlingService.avbrytKlagebehandling(
                        kommando = AvbrytKlagebehandlingKommando(
                            sakId = sakId,
                            klagebehandlingId = klagebehandlingId,
                            begrunnelse = NonBlankString.create(it.begrunnelse),
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(it.toStatusAndErrorJson())
                        },
                        ifRight = { (sak, behandling) ->
                            val behandlingId = behandling.id
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Avbryter klagebehandling på sak $sakId",
                                correlationId = correlationId,
                                behandlingId = behandlingId,
                            )
                            /*
                            returnerer sak fordi vi oppdaterer klagen 2 forskjellige plasser i json.
                             */
                            call.respondJson(value = sak.toSakDTO(clock))
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeAvbryteKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeAvbryteKlagebehandling.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(
                this.forventetSaksbehandler,
            ),
        )

        is KanIkkeAvbryteKlagebehandling.KnyttetTilIkkeAvbruttRammebehandling -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Klagebehandlingen kan ikke avbrytes fordi den er knyttet til en rammebehandling som ikke er avbrutt: ${this.rammebehandlingId}",
                "knyttet_til_ikke_avbrutt_rammebehandling",
            ),
        )

        is KanIkkeAvbryteKlagebehandling.AlleredeAvsluttet -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Klagebehandlingen er allerede avsluttet med status: ${this.status}",
                "allerede_avsluttet",
            ),
        )
    }
}
