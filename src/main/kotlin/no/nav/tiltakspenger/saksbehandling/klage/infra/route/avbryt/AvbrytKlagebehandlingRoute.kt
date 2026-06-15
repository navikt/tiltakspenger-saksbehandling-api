package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import arrow.core.Either
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbruttKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.service.AvbrytKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.toSakDTO
import java.time.Clock

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/avbryt"

data class AvbrytKlagebehandlingBody(
    val status: AvbruttKlagebehandlingStatus,
    val begrunnelse: String?,
) {

    fun toCommand(
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): Either<Pair<HttpStatusCode, ErrorJson>, AvbrytKlagebehandlingKommando> {
        val begrunnelse = when (status) {
            AvbruttKlagebehandlingStatus.KLAGE_TRUKKET,
            AvbruttKlagebehandlingStatus.FEILREGISTRER_KLAGE,
            AvbruttKlagebehandlingStatus.MANGLENDE_UTBETALING,
            -> {
                if (begrunnelse != null) {
                    return Either.Left(
                        HttpStatusCode.BadRequest to ErrorJson(
                            melding = "Begrunnelse må være null når status ikke er ANNET",
                            kode = "ugyldig_begrunnelse_for_status",
                        ),
                    )
                }
                null
            }

            AvbruttKlagebehandlingStatus.ANNET -> Either.catch { begrunnelse?.toNonBlankString() }.getOrNull() ?: return Either.Left(
                HttpStatusCode.BadRequest to ErrorJson(
                    melding = "Begrunnelse må være satt når status er ANNET",
                    kode = "begrunnelse_må_være_satt_for_status",
                ),
            )
        }

        return AvbrytKlagebehandlingKommando(
            status = status,
            begrunnelse = begrunnelse,
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
        ).right()
    }
}

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
                call.withBody<AvbrytKlagebehandlingBody> { body ->

                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)

                    body.toCommand(
                        sakId = sakId,
                        klagebehandlingId = klagebehandlingId,
                        correlationId = correlationId,
                        saksbehandler = saksbehandler,
                    ).fold(
                        ifLeft = {
                            call.respondJson(it.second, it.first)
                            return@withBody
                        },
                        ifRight = {
                            avbrytKlagebehandlingService.avbrytKlagebehandling(kommando = it)
                                .fold(
                                    ifLeft = { call.respondJson(it.toStatusAndErrorJson()) },
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
                                        call.respondJson(value = sak.toSakDTO(saksbehandler, clock))
                                    },
                                )
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeAvbryteKlagebehandling.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KanIkkeAvbryteKlagebehandling.BehandlingenErSattPåVent -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Klagebehandlingen er satt på vent. Den må gjenopptas før den kan behandles videre.",
                "klagebehandling_er_satt_p_vent",
            ),
        )

        is KanIkkeAvbryteKlagebehandling.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(
                this.forventetSaksbehandler,
            ),
        )

        is KanIkkeAvbryteKlagebehandling.KnyttetTilIkkeAvbruttBehandling -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Klagebehandlingen kan ikke avbrytes fordi den er knyttet til en behandling som ikke er avbrutt: ${this.behandlingId}",
                "knyttet_til_ikke_avbrutt_behandling",
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
