package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppretteOmgjøring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

private const val PATH = "/sak/{sakId}/revurdering/start"

fun Route.startRevurderingRoute(
    startRevurderingService: StartRevurderingService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Oppretter en ny revurdering" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBody<StartRevurderingBody> { body ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                startRevurderingService.startRevurdering(
                    kommando = body.tilKommando(
                        sakId,
                        saksbehandler,
                        correlationId,
                    ),
                ).fold(
                    ifLeft = {
                        call.respondJson(valueAndStatus = it.tilStatusOgErrorJson())
                    },
                    ifRight = { (sak, behandling) ->
                        val behandlingId = behandling.id
                        auditService.logMedSakId(
                            sakId = sakId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.CREATE,
                            contextMessage = "Oppretter revurdering på sak $sakId",
                            correlationId = correlationId,
                            behandlingId = behandlingId,
                        )
                        call.respondJson(value = sak.tilRammebehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}

internal fun KunneIkkeStarteRevurdering.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> =
    when (this) {
        is KunneIkkeStarteRevurdering.Omgjøring -> when (this.årsak) {
            KunneIkkeOppretteOmgjøring.KanKunStarteOmgjøringDersomViKanInnvilgeMinst1Dag -> Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Kan kun starte omgjøring dersom vi kan innvilge minst en dag. En ren opphørsomgjøring kommer senere.",
                    "kan_kun_starte_omgjøring_dersom_vi_kan_innvilge_minst_1_dag",
                ),
            )
        }
    }

private data class StartRevurderingBody(
    val revurderingType: RammebehandlingResultatTypeDTO,
    val rammevedtakIdSomOmgjøres: String? = null,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): StartRevurderingKommando {
        return StartRevurderingKommando(
            sakId = sakId,
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            revurderingType = revurderingType.tilRevurderingType(),
            vedtakIdSomOmgjøres = rammevedtakIdSomOmgjøres?.let { VedtakId.fromString(it) },
            klagebehandlingId = null,
        )
    }
}
