package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettBehandling

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
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.ktor.common.withSakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil.behandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.route.withKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.service.KanIkkeOppretteBehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettBehandlingForKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettBehandlingForKlageResultat
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettBehandlingForKlageService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettMeldekortbehandlingForKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettRevurderingForKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettSøknadsbehandlingForKlageKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.v2.tilMeldekortbehandlingDTOV2
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling
import java.time.Clock

private data class OpprettBehandlingForKlageRequest(
    val søknadId: String? = null,
    val type: Type,
    val vedtakIdSomSkalOmgjøres: String? = null,
    val kjedeId: String? = null,
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
    ): OpprettBehandlingForKlageKommando {
        return when (type) {
            Type.SØKNADSBEHANDLING_INNVILGELSE -> OpprettSøknadsbehandlingForKlageKommando(
                sakId = sakId,
                saksbehandler = saksbehandler,
                klagebehandlingId = klagebehandlingId,
                søknadId = SøknadId.fromString(søknadId!!),
                correlationId = correlationId,
            )

            Type.REVURDERING_INNVILGELSE, Type.REVURDERING_OMGJØRING,
            -> OpprettRevurderingForKlageKommando(
                sakId = sakId,
                saksbehandler = saksbehandler,
                klagebehandlingId = klagebehandlingId,
                type = when (type) {
                    Type.REVURDERING_INNVILGELSE -> OpprettRevurderingForKlageKommando.Type.INNVILGELSE
                    Type.REVURDERING_OMGJØRING -> OpprettRevurderingForKlageKommando.Type.OMGJØRING
                },
                correlationId = correlationId,
                vedtakIdSomOmgjøres = when (type) {
                    Type.REVURDERING_INNVILGELSE -> null
                    Type.REVURDERING_OMGJØRING -> VedtakId.fromString(vedtakIdSomSkalOmgjøres!!)
                },
            )

            Type.MELDEKORTBEHANDLING -> OpprettMeldekortbehandlingForKlageKommando(
                sakId = sakId,
                klagebehandlingId = klagebehandlingId,
                kjedeId = MeldeperiodeKjedeId(kjedeId!!),
                saksbehandler = saksbehandler,
                correlationId = correlationId,
            )
        }
    }
}

private const val OPPRETT_BEHANDLING_FOR_KLAGE_PATH = "/sak/{sakId}/klage/{klagebehandlingId}/opprettBehandling"

fun Route.opprettBehandlingForKlageRoute(
    opprettBehandlingForKlageService: OpprettBehandlingForKlageService,
    auditService: AuditService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    val logger = KotlinLogging.logger {}

    post(OPPRETT_BEHANDLING_FOR_KLAGE_PATH) {
        logger.debug { "Mottatt post-request på '$OPPRETT_BEHANDLING_FOR_KLAGE_PATH' - Oppretter behandling fra klage" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withKlagebehandlingId { klagebehandlingId ->
                call.withBody<OpprettBehandlingForKlageRequest> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    opprettBehandlingForKlageService.opprett(
                        kommando = body.tilKommando(
                            sakId = sakId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                            klagebehandlingId = klagebehandlingId,
                        ),
                    ).fold(
                        ifLeft = {
                            when (val feil = it.toStatusAndErrorJson()) {
                                null -> call.respond500InternalServerError(
                                    melding = "Kunne ikke hente Nav-kontor for brukeren",
                                    kode = "kunne_ikke_hente_navkontor",
                                )

                                else -> call.respondJson(feil)
                            }
                        },
                        ifRight = {
                            when (it) {
                                is OpprettBehandlingForKlageResultat.RammebehandlingOpprettet -> {
                                    val klagebehandling = it.rammebehandling.klagebehandling!!
                                    val klagebehandlingId = klagebehandling.id
                                    val rammebehandlingId = it.rammebehandling.id
                                    auditService.logMedSakId(
                                        sakId = sakId,
                                        navIdent = saksbehandler.navIdent,
                                        action = AuditLogEvent.Action.UPDATE,
                                        contextMessage = "Opprettet rammebehandling $rammebehandlingId fra klagebehandling $klagebehandlingId på sak $sakId",
                                        correlationId = correlationId,
                                        behandlingId = rammebehandlingId,
                                    )
                                    call.respondJson(value = it.sak.tilRammebehandlingDTO(rammebehandlingId))
                                }

                                is OpprettBehandlingForKlageResultat.MeldekortbehandlingOpprettet -> {
                                    auditService.logMedSakId(
                                        sakId = sakId,
                                        navIdent = saksbehandler.navIdent,
                                        action = AuditLogEvent.Action.CREATE,
                                        contextMessage = "Opprettet meldekortbehandling ${it.meldekortbehandling.id} fra klagebehandling $klagebehandlingId på sak $sakId",
                                        correlationId = correlationId,
                                        behandlingId = it.meldekortbehandling.id,
                                    )
                                    call.respondJson(
                                        value = it.meldekortbehandling.tilMeldekortbehandlingDTOV2(
                                            beregninger = it.sak.meldeperiodeBeregninger,
                                            hentVedtak = it.sak.meldekortvedtaksliste::hentForMeldekortbehandling,
                                            hentTilbakekreving = it.sak::hentTilbakekrevingForMeldekortbehandling,
                                        ),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun KanIkkeOppretteBehandlingForKlage.toStatusAndErrorJson(): Pair<HttpStatusCode, ErrorJson>? {
    return when (this) {
        is KanIkkeOppretteBehandlingForKlage.KanIkkeOppretteMeldekortbehandling -> when (underliggende) {
            KanIkkeOppretteMeldekortbehandling.HenteNavKontorFeilet -> null

            is KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil -> Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Meldeperiodekjeden er i en tilstand som ikke tillater å opprette en behandling: ${underliggende.feil}",
                    underliggende.feil.toString(),
                ),
            )

            is KanIkkeOppretteMeldekortbehandling.SaksbehandlerMismatch -> Pair(
                HttpStatusCode.BadRequest,
                behandlingenEiesAvAnnenSaksbehandler(underliggende.forventetSaksbehandler),
            )

            is KanIkkeOppretteMeldekortbehandling.FinnesÅpenBehandling -> Pair(
                HttpStatusCode.BadRequest,
                ErrorJson(
                    "Det finnes allerede en åpen behandling ${underliggende.behandlingId} for denne klagebehandlingen.",
                    "finnes_åpen_behandling",
                ),
            )
        }

        is KanIkkeOppretteBehandlingForKlage.SaksbehandlerMismatch -> Pair(
            HttpStatusCode.BadRequest,
            behandlingenEiesAvAnnenSaksbehandler(forventetSaksbehandler),
        )

        is KanIkkeOppretteBehandlingForKlage.FinnesÅpenBehandling -> Pair(
            HttpStatusCode.BadRequest,
            ErrorJson(
                "Det finnes allerede en åpen behandling $behandlingId for denne klagebehandlingen.",
                "finnes_åpen_behandling",
            ),
        )
    }
}
