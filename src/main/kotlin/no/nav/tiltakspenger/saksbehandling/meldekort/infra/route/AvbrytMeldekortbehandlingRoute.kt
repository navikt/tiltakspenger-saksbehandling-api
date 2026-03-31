package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
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
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.AvbrytMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.KanIkkeAvbryteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AvbrytMeldekortbehandlingService

private const val AVBRYT_MELDEKORTBEHANDLING_PATH = "/sak/{sakId}/meldekort/{meldekortId}/avbryt"

data class AvbrytMeldekortbehandlingBody(
    val begrunnelse: String,
)

fun Route.avbrytMeldekortbehandlingRoute(
    auditService: AuditService,
    avbrytMeldekortbehandlingService: AvbrytMeldekortbehandlingService,
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
                    avbrytMeldekortbehandlingService.avbryt(
                        AvbrytMeldekortbehandlingKommando(
                            sakId = sakId,
                            meldekortId = meldekortId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            begrunnelse = body.begrunnelse.toNonBlankString(),
                        ),
                    ).fold(
                        {
                            call.respondJson(statusAndValue = it.tilStatusOgErrorJson())
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
                                value = behandling.tilMeldekortbehandlingDTO(beregninger = sak.meldeperiodeBeregninger),
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeAvbryteMeldekortbehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KanIkkeAvbryteMeldekortbehandling.MåVæreSaksbehandlerForMeldekortet -> HttpStatusCode.BadRequest to ErrorJson(
            "Meldekortbehandlingen er tildelt en annen saksbehandler",
            "behandlingen_tildelt_annen_saksbehandler",
        )

        KanIkkeAvbryteMeldekortbehandling.MåVæreUnderBehandling -> HttpStatusCode.BadRequest to ErrorJson(
            "Meldekortbehandlingen må være under behandling for å kunne avbrytes",
            "behandlingen_ikke_under_behandling",
        )
    }
}
