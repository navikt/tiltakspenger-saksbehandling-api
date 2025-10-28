package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.server.auth.principal
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.texas.TexasPrincipalInternal
import no.nav.tiltakspenger.libs.texas.saksbehandler
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toAvslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.toDomain
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.repo.correlationId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBehandlingId
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.repo.withSakId
import java.time.LocalDate

/**
 * @param avslagsgrunner Brukes kun ved søknadsbehandling avslag
 * @param valgteHjemler Brukes kun ved revurdering til stans
 * @param stansFraOgMed Brukes kun ved revurdering til stans
 * @param stansTilOgMed Brukes kun ved revurdering til stans
 * @param barnetillegg Brukes ved innvilgelse (søknadsbehandling+revurdering). Kan inneholde hull. Må valideres basert på innsendt virkningsperiode eller virkningsperioden på behandlingen.
 * @param virkningsperiode Brukes ved avslag og innvilgelse (søknadsbehandling+revurdering). Brukes kun hvis den ikke er satt på behandlingen.
 */
private data class Body(
    val fritekst: String,
    val virkningsperiode: PeriodeDTO?,
    val harValgtStansFraFørsteDagSomGirRett: Boolean?,
    val harValgtStansTilSisteDagSomGirRett: Boolean?,
    val stansFraOgMed: LocalDate?,
    val stansTilOgMed: LocalDate?,
    val valgteHjemler: List<ValgtHjemmelForStansDTO>?,
    val barnetillegg: List<BarnetilleggPeriodeDTO>?,
    val resultat: RammebehandlingResultatTypeDTO,
    val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>?,
) {
    init {
        if (harValgtStansFraFørsteDagSomGirRett == true) require(stansFraOgMed == null) { "stansFraOgMed må være null når harValgtStansFraFørsteDagSomGirRett er true" }
        if (harValgtStansTilSisteDagSomGirRett == true) require(stansTilOgMed == null) { "stansTilOgMed må være null når harValgtStansTilSisteDagSomGirRett er true" }
        if (harValgtStansFraFørsteDagSomGirRett == false) requireNotNull(stansFraOgMed) { "stansFraOgMed kan ikke være null når harValgtStansFraFørsteDagSomGirRett er false" }
        if (harValgtStansTilSisteDagSomGirRett == false) requireNotNull(stansTilOgMed) { "stansTilOgMed kan ikke være null når harValgtStansTilSisteDagSomGirRett er false" }
    }
    fun toDomain(
        sakId: SakId,
        behandlingId: BehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): ForhåndsvisVedtaksbrevKommando {
        val virkningsperiode = virkningsperiode?.toDomain()
        val resultat = resultat.toDomain()

        requireNotNull(resultat) {
            "Behandlingen må ha et valgt resultat for å generere brev"
        }

        return ForhåndsvisVedtaksbrevKommando(
            fritekstTilVedtaksbrev = FritekstTilVedtaksbrev(saniter(fritekst)),
            sakId = sakId,
            behandlingId = behandlingId,
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            virkningsperiode = virkningsperiode,
            valgteHjemler = valgteHjemler?.toDomain(),
            stansFraOgMed = stansFraOgMed,
            stansTilOgMed = stansTilOgMed,
            barnetillegg = if (barnetillegg == null || barnetillegg.isEmpty()) null else (barnetillegg.tilPeriodisering() as IkkeTomPeriodisering),
            resultat = resultat,
            avslagsgrunner = this.avslagsgrunner?.toAvslagsgrunnlag(),
        )
    }
}

/**
 * Brukes for søknadsbehandling (innvilgelse+avslag) + revurdering (innvilgelse+stans).
 */
fun Route.forhåndsvisVedtaksbrevRoute(
    auditService: AuditService,
    forhåndsvisVedtaksbrevService: ForhåndsvisVedtaksbrevService,
    tilgangskontrollService: TilgangskontrollService,
) {
    val logger = KotlinLogging.logger {}
    post("/sak/{sakId}/behandling/{behandlingId}/forhandsvis") {
        logger.debug { "Mottatt post-request på '/sak/{sakId}/behandling/{behandlingId}/forhandsvis' - forhåndsviser vedtaksbrev" }
        val token = call.principal<TexasPrincipalInternal>()?.token ?: return@post
        val saksbehandler = call.saksbehandler(autoriserteBrukerroller()) ?: return@post
        call.withSakId { sakId ->
            call.withBehandlingId { behandlingId ->
                call.withBody<Body> { body ->
                    val correlationId = call.correlationId()
                    krevSaksbehandlerEllerBeslutterRolle(saksbehandler)
                    tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, token)
                    forhåndsvisVedtaksbrevService.forhåndsvisVedtaksbrev(
                        body.toDomain(
                            sakId = sakId,
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                            correlationId = correlationId,
                        ),
                    ).also {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "forhåndsviser vedtaksbrev",
                            correlationId = correlationId,
                        )
                        call.respondBytes(it.getContent(), ContentType.Application.Pdf)
                    }
                }
            }
        }
    }
}
