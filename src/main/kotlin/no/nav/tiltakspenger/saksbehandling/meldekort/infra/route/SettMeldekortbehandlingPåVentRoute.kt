package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.KanIkkeSetteMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SettMeldekortbehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock
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
    clock: Clock,
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
                    ).fold(
                        { call.respondSettPåVentError(it) },
                        { (sak, _) ->
                            auditService.logMedMeldekortId(
                                meldekortId = meldekortId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Meldekortbehandling er satt på vent",
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

private suspend fun ApplicationCall.respondSettPåVentError(
    feil: KanIkkeSetteMeldekortbehandlingPåVent,
) {
    when (feil) {
        is KanIkkeSetteMeldekortbehandlingPåVent.BehandlingenErAlleredePåVent -> respond400BadRequest(
            melding = "Meldekortbehandlingen er allerede satt på vent.",
            kode = "behandlingen_er_allerede_paa_vent",
        )

        is KanIkkeSetteMeldekortbehandlingPåVent.MåVæreSaksbehandler -> respond403Forbidden(
            melding = "Du må være saksbehandler for å sette denne meldekortbehandlingen på vent.",
            kode = "maa_vaere_saksbehandler",
        )

        is KanIkkeSetteMeldekortbehandlingPåVent.MåVæreSaksbehandlerForMeldekortet -> respond403Forbidden(
            melding = "Du må være saksbehandleren som er tildelt meldekortbehandlingen for å sette den på vent.",
            kode = "maa_vaere_saksbehandler_for_meldekortet",
        )

        is KanIkkeSetteMeldekortbehandlingPåVent.MåVæreBeslutter -> respond403Forbidden(
            melding = "Du må være beslutter for å sette denne meldekortbehandlingen på vent.",
            kode = "maa_vaere_beslutter",
        )

        is KanIkkeSetteMeldekortbehandlingPåVent.MåVæreBeslutterForMeldekortet -> respond403Forbidden(
            melding = "Du må være beslutteren som er tildelt meldekortbehandlingen for å sette den på vent.",
            kode = "maa_vaere_beslutter_for_meldekortet",
        )

        is KanIkkeSetteMeldekortbehandlingPåVent.UgyldigStatus -> respond400BadRequest(
            melding = "Kan ikke sette meldekortbehandling med status ${feil.status} på vent.",
            kode = "ugyldig_status_for_sett_paa_vent",
        )
    }
}
