package no.nav.tiltakspenger.saksbehandling.behandling.infra.route
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingMedSakId
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingerKommando
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import java.time.Clock

private const val TA_FLERE_BEHANDLING_PATH = "/behandlinger/ta"

private data class TaFlereRammebehandlinger(
    val behandlinger: List<BehandlingMedSakIdBody>,
) {
    fun tilKommando(saksbehandler: Saksbehandler): TaRammebehandlingerKommando {
        return TaRammebehandlingerKommando(
            saksbehandler = saksbehandler,
            behandlinger = behandlinger.map {
                RammebehandlingMedSakId(
                    behandlingId = RammebehandlingId.fromString(it.behandlingId),
                    sakId = SakId.fromString(it.sakId),
                )
            },
        )
    }

    data class BehandlingMedSakIdBody(
        val behandlingId: String,
        val sakId: String,
    )
}

fun Route.taFlereRammebehandlingerRoute(
    auditService: AuditService,
    taBehandlingService: TaRammebehandlingService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    post(TA_FLERE_BEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$TA_FLERE_BEHANDLING_PATH' - Knytter saksbehandler/beslutter til flere behandlinger." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withBody<TaFlereRammebehandlinger> { body ->

            val kommando = body.tilKommando(saksbehandler)

            val correlationId = call.correlationId()
            krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

            kommando.behandlinger.distinctBy { it.sakId }.forEach { tilgangskontrollService.harTilgangTilPersonForSakId(it.sakId, saksbehandler, token) }
        }
    }
}
