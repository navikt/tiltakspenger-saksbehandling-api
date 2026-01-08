package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId

private const val PATH = "/sak/{sakId}/behandling/{behandlingId}/saksopplysninger"

fun Route.oppdaterSaksopplysningerRoute(
    auditService: AuditService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    patch(PATH) {
        logger.debug { "Mottatt patch-request på '$PATH' - henter saksopplysninger fra registre på nytt og oppdaterer behandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                krevSaksbehandlerRolle(saksbehandler)
                tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                oppdaterSaksopplysningerService.oppdaterSaksopplysninger(
                    sakId,
                    behandlingId,
                    saksbehandler,
                    correlationId,
                ).fold(
                    ifLeft = {
                        call.respondJson(valueAndStatus = it.tilStatusOgErrorJson())
                    },
                    ifRight = { (sak) ->
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Oppdaterer saksopplysninger",
                            correlationId = correlationId,
                        )

                        call.respondJson(value = sak.tilBehandlingDTO(behandlingId))
                    },
                )
            }
        }
    }
}

internal fun KunneIkkeOppdatereSaksopplysninger.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KunneIkkeOppdatereSaksopplysninger.KunneIkkeOppdatereBehandling -> this.valideringsfeil.tilStatusOgErrorJson()
    KunneIkkeOppdatereSaksopplysninger.KanKunStarteOmgjøringDersomViKanInnvilgeMinst1Dag -> Pair(
        HttpStatusCode.Forbidden,
        ErrorJson(
            "Kan kun oppdatere omgjøring dersom vi kan innvilge minst en dag. En ren opphørsomgjøring kommer senere.",
            "kan_kun_oppdatere_omgjøring_dersom_vi_kan_innvilge_minst_1_dag",
        ),
    )
}
