package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMeldekortId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeAvbryteMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AvbrytMeldekortBehandlingService

private const val AVBRYT_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/avbryt"

data class AvbrytMeldekortbehandlingBody(
    val begrunnelse: String,
)

fun Route.avbrytMeldekortBehandlingRoute(
    auditService: AuditService,
    avbrytMeldekortBehandlingService: AvbrytMeldekortBehandlingService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post(AVBRYT_MELDEKORTBEHANDLING_PATH) {
        logger.debug { "Mottatt post-request på '$AVBRYT_MELDEKORTBEHANDLING_PATH' - avbryter meldekortbehandlingen." }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withMeldekortId { meldekortId ->
                call.withBody<AvbrytMeldekortbehandlingBody> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    avbrytMeldekortBehandlingService.avbryt(
                        AvbrytMeldekortBehandlingCommand(
                            sakId = sakId,
                            meldekortId = meldekortId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            begrunnelse = body.begrunnelse,
                        ),
                    ).fold(
                        {
                            call.respondJson(valueAndStatus = it.tilStatusOgErrorJson())
                        },
                        { (sak, behandling) ->
                            auditService.logMedMeldekortId(
                                meldekortId = meldekortId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Saksbehandler avbryter meldekortbehandlingen",
                                correlationId = correlationId,
                            )

                            call.respondJson(
                                value = behandling.tilMeldekortBehandlingDTO(beregninger = sak.meldeperiodeBeregninger),
                            )
                        },
                    )
                }
            }
        }
    }
}

internal fun KanIkkeAvbryteMeldekortBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KanIkkeAvbryteMeldekortBehandling.MåVæreSaksbehandlerForMeldekortet -> HttpStatusCode.BadRequest to ErrorJson(
            "Meldekortbehandlingen er tildelt en annen saksbehandler",
            "behandlingen_tildelt_annen_saksbehandler",
        )

        KanIkkeAvbryteMeldekortBehandling.MåVæreUnderBehandling -> HttpStatusCode.BadRequest to ErrorJson(
            "Meldekortbehandlingen må være under behandling for å kunne avbrytes",
            "behandlingen_ikke_under_behandling",
        )
    }
}

data class AvbrytMeldekortBehandlingCommand(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val begrunnelse: String,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
