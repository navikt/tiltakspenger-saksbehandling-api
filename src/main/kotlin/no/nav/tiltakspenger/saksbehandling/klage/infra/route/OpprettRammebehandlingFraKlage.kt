package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.KanIkkeOppretteRammebehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettRammebehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettRevurderingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettSøknadsbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettRammebehandlingFraKlageService

private data class OpprettRammebehandlingFraKlage(
    val søknadId: String?,
    val vedtakIdSomOmgjøres: String?,
    val type: Type,
) {
    enum class Type {
        SØKNADSBEHANDLING_INNVILGELSE,
        REVURDERING_INNVILGELSE,
        REVURDERING_OMGJØRING,
    }

    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        klagebehandlingId: KlagebehandlingId,
    ): OpprettRammebehandlingFraKlageKommando {
        return when (type) {
            Type.SØKNADSBEHANDLING_INNVILGELSE -> {
                OpprettSøknadsbehandlingFraKlageKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    klagebehandlingId = klagebehandlingId,
                    søknadId = SøknadId.fromString(søknadId!!),
                    correlationId = correlationId,
                )
            }

            Type.REVURDERING_INNVILGELSE, Type.REVURDERING_OMGJØRING -> {
                OpprettRevurderingFraKlageKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    klagebehandlingId = klagebehandlingId,
                    type = when (type) {
                        Type.REVURDERING_INNVILGELSE -> OpprettRevurderingFraKlageKommando.Type.INNVILGELSE
                        Type.REVURDERING_OMGJØRING -> OpprettRevurderingFraKlageKommando.Type.OMGJØRING
                    },
                    correlationId = correlationId,
                    vedtakIdSomOmgjøres = when (type) {
                        Type.REVURDERING_INNVILGELSE -> null
                        Type.REVURDERING_OMGJØRING -> VedtakId.fromString(vedtakIdSomOmgjøres!!)
                    },
                )
            }
        }
    }
}

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/opprettRammebehandling"

fun Route.opprettRammebehandlingFraKlage(
    opprettRammebehandlingFraKlageService: OpprettRammebehandlingFraKlageService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Oppretter rammebehandling fra klage" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<OpprettRammebehandlingFraKlage> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    opprettRammebehandlingFraKlageService.opprett(
                        kommando = body.tilKommando(
                            sakId = sakId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            klagebehandlingId = klagebehandlingId,
                        ),
                    ).fold(
                        ifLeft = {
                            call.respondJson(it.toStatusAndErrorJson())
                        },
                        ifRight = { (sak, klagebehandling, rammebehandling) ->
                            val klagebehandlingId = klagebehandling.id
                            val rammebehandlingId = rammebehandling.id
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Opprettet rammebehandling $rammebehandlingId fra klagebehandling $klagebehandlingId på sak $sakId",
                                correlationId = correlationId,
                                behandlingId = rammebehandlingId,
                            )
                            call.respondJson(value = sak.tilRammebehandlingDTO(rammebehandlingId))
                        },
                    )
                }
            }
        }
    }
}

fun KanIkkeOppretteRammebehandlingFraKlage.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeOppretteRammebehandlingFraKlage.SaksbehandlerMismatch -> {
            Pair(
                HttpStatusCode.BadRequest,
                behandlingenEiesAvAnnenSaksbehandler(
                    this.forventetSaksbehandler,
                ),
            )
        }

        is KanIkkeOppretteRammebehandlingFraKlage.FinnesÅpenRammebehandling -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Det finnes allerede en åpen rammebehandling ${this.rammebehandlingId} for denne klagebehandlingen.",
                    "finnes_åpen_rammebehandling",
                ),
            )
        }
    }
}
