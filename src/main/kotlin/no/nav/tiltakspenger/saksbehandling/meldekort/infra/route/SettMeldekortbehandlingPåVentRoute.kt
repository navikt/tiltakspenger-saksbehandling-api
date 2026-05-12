package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SettMeldekortbehandlingPåVentService
import java.time.LocalDate

private const val SETT_MELDEKORTBEHANDLING_PÅ_VENT_PATH = "/sak/{sakId}/meldekort/{meldekortId}/vent"

private data class SettMeldekortbehandlingPåVentBody(
    val begrunnelse: String,
    val frist: LocalDate?,
) {
    fun tilKommando(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ) = SettMeldekortbehandlingPåVentKommando(
        sakId = sakId,
        meldekortId = meldekortId,
        saksbehandler = saksbehandler,
        begrunnelse = begrunnelse,
        frist = frist,
        correlationId = correlationId,
    )
}

fun Route.settMeldekortbehandlingPåVentRoute(
    auditService: AuditService,
    settMeldekortbehandlingPåVentService: SettMeldekortbehandlingPåVentService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    patch(SETT_MELDEKORTBEHANDLING_PÅ_VENT_PATH) {
        logger.debug { "Mottatt patch-request på '$SETT_MELDEKORTBEHANDLING_PÅ_VENT_PATH' - setter meldekortbehandling på vent." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@patch
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@patch
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                call.withBody<SettMeldekortbehandlingPåVentBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    settMeldekortbehandlingPåVentService.settPåVent(
                        kommando = body.tilKommando(
                            sakId = sakId,
                            meldekortId = meldekortId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).also { (sak, behandling) ->
                        auditService.logMedMeldekortId(
                            meldekortId = meldekortId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Meldekortbehandling er satt på vent",
                            correlationId = correlationId,
                        )

                        call.respondJson(
                            value = behandling.tilMeldekortbehandlingDTO(beregninger = sak.meldeperiodeBeregninger),
                        )
                    }
                }
            }
        }
    }
}
