package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.KanIkkeOppretteBehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettMeldekortbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettRevurderingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettSøknadsbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettRammebehandlingFraKlageService
import no.nav.tiltakspenger.saksbehandling.sak.Sak

private data class OpprettBehandlingFraKlage(
    val søknadId: String?,
    val type: Type,
    val vedtakIdSomSkalOmgjøres: String?,
) {
    enum class Type {
        SØKNADSBEHANDLING_INNVILGELSE,
        REVURDERING_INNVILGELSE,
        REVURDERING_OMGJØRING,
        MELDEKORTBEHANDLING,
    }

    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        klagebehandlingId: KlagebehandlingId,
    ): OpprettbehandlingFraKlageKommando {
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
                        Type.REVURDERING_OMGJØRING -> VedtakId.fromString(vedtakIdSomSkalOmgjøres!!)
                    },
                )
            }

            Type.MELDEKORTBEHANDLING -> OpprettMeldekortbehandlingFraKlageKommando(
                sakId = sakId,
                saksbehandler = saksbehandler,
                klagebehandlingId = klagebehandlingId,
                correlationId = correlationId,
            )
        }
    }
}

private const val PATH = "/sak/{sakId}/klage/{klagebehandlingId}/opprettBehandling"

fun Route.opprettBehandlingFraKlageRoute(
    opprettRammebehandlingFraKlageService: OpprettRammebehandlingFraKlageService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}

    post(PATH) {
        logger.debug { "Mottatt post-request på '$PATH' - Oppretter behandling fra klage" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<OpprettBehandlingFraKlage> { body ->
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
                        ifRight = { (sak, behandling) ->
                            val klagebehandling = behandling.klagebehandling!!
                            val klagebehandlingId = klagebehandling.id
                            val behandlingId = behandling.id
                            auditService.logMedSakId(
                                sakId = sakId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.UPDATE,
                                contextMessage = "Opprettet behandling $behandlingId fra klagebehandling $klagebehandlingId på sak $sakId",
                                correlationId = correlationId,
                                behandlingId = behandlingId,
                            )

                            call.respondJson(value = sak.tilOpprettetBehandlingFraKlageDto(behandlingId))
                        },
                    )
                }
            }
        }
    }
}

data class OpprettetBehandlingFraKlageDto(
    val sakId: String,
    val behandlingId: String,
    val type: Type,
) {
    companion object {
        enum class Type {
            RAMMEBEHANDLING,
            MELDEKORTBEHANDLING,
        }
    }
}

private fun Sak.tilOpprettetBehandlingFraKlageDto(behandlingId: BehandlingId): OpprettetBehandlingFraKlageDto {
    if (behandlingId.prefixPart().startsWith(RammebehandlingId.PREFIX)) {
        return OpprettetBehandlingFraKlageDto(
            sakId = this.id.toString(),
            behandlingId = behandlingId.toString(),
            type = OpprettetBehandlingFraKlageDto.Companion.Type.RAMMEBEHANDLING,
        )
    }

    if (behandlingId.prefixPart().startsWith(MeldekortId.PREFIX)) {
        return OpprettetBehandlingFraKlageDto(
            sakId = this.id.toString(),
            behandlingId = behandlingId.toString(),
            type = OpprettetBehandlingFraKlageDto.Companion.Type.MELDEKORTBEHANDLING,
        )
    }

    throw IllegalArgumentException("Ukjent behandlingstype for behandlingId: $behandlingId")
}

fun KanIkkeOppretteBehandlingFraKlage.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KanIkkeOppretteBehandlingFraKlage.SaksbehandlerMismatch -> {
            Pair(
                HttpStatusCode.BadRequest,
                behandlingenEiesAvAnnenSaksbehandler(
                    this.forventetSaksbehandler,
                ),
            )
        }

        is KanIkkeOppretteBehandlingFraKlage.`FinnesÅpenBehandling` -> {
            Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Det finnes allerede en åpen rammebehandling ${this.behandlingId} for denne klagebehandlingen.",
                    "finnes_åpen_rammebehandling",
                ),
            )
        }
    }
}
