package no.nav.tiltakspenger.vedtak.routes.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.ktor.withSaksbehandler
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppdatereTilleggstekstBrev
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppdatereVurderingsperiode
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterTilleggstekstBrevKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterVurderingsperiodeKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.TilleggstekstBrev
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.kvp.KvpVilkårService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.tiltaksdeltagelse.TiltaksdeltagelseVilkårService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.auditlog.AuditLogEvent
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.personopplysninger.hentPersonRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.stønadsdager.stønadsdagerRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder.alderRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.institusjonsopphold.institusjonsoppholdRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.introduksjonsprogrammet.introRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist.kravfristRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.kvpRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold.livsoppholdRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.tiltakdeltagelse.tiltakDeltagelseRoutes
import no.nav.tiltakspenger.vedtak.routes.correlationId
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.behandlingKanIkkeVæreSendtTilBeslutterEllerVedtatt
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.ikkeTilgang
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandler
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.måVæreSaksbehandlerEllerBeslutter
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.Standardfeil.nyPeriodeMåVæreInnenforVurderingsperiode
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond400BadRequest
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond403Forbidden
import no.nav.tiltakspenger.vedtak.routes.withBehandlingId
import no.nav.tiltakspenger.vedtak.routes.withBody

internal const val BEHANDLING_PATH = "/behandling"
internal const val BEHANDLINGER_PATH = "/behandlinger"

private data class OppdaterVurderingsPeriodeBody(
    val periode: PeriodeDTO,
) {
    fun tilKommando(
        behandlingId: BehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): OppdaterVurderingsperiodeKommando {
        return OppdaterVurderingsperiodeKommando(
            behandlingId = behandlingId,
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            periode = periode.toDomain(),
        )
    }
}

private data class OppdaterSubsumsjonBody(
    val subsumsjon: TilleggstekstBrev.Subsumsjon,
) {
    fun tilKommando(
        behandlingId: BehandlingId,
        correlationId: CorrelationId,
        saksbehandler: Saksbehandler,
    ): OppdaterTilleggstekstBrevKommando {
        return OppdaterTilleggstekstBrevKommando(
            behandlingId = behandlingId,
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            subsumsjon = subsumsjon,
        )
    }
}

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tiltaksdeltagelseVilkårService: TiltaksdeltagelseVilkårService,
    tokenService: TokenService,
    sakService: SakService,
    kvpVilkårService: KvpVilkårService,
    livsoppholdVilkårService: LivsoppholdVilkårService,
    auditService: AuditService,
) {
    val logger = KotlinLogging.logger {}
    get("$BEHANDLING_PATH/{behandlingId}") {
        logger.debug("Mottatt get-request på '$BEHANDLING_PATH/{behandlingId}' - henter hele behandlingen")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                behandlingService.hentBehandlingForSaksbehandler(behandlingId, saksbehandler, correlationId).fold(
                    {
                        call.respond403Forbidden(måVæreSaksbehandlerEllerBeslutter())
                    },
                    {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.ACCESS,
                            contextMessage = "Henter hele behandlingen",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, it.toDTO())
                    },
                )
            }
        }
    }

    post("$BEHANDLING_PATH/beslutter/{behandlingId}") {
        logger.debug("Mottatt post-request på '$BEHANDLING_PATH/beslutter/{behandlingId}' - sender behandling til beslutter")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                behandlingService.sendTilBeslutter(behandlingId, saksbehandler, correlationId).fold(
                    { call.respond403Forbidden(måVæreSaksbehandler()) },
                    {
                        auditService.logMedBehandlingId(
                            behandlingId = behandlingId,
                            navIdent = saksbehandler.navIdent,
                            action = AuditLogEvent.Action.UPDATE,
                            contextMessage = "Sender behandlingen til beslutter",
                            correlationId = correlationId,
                        )

                        call.respond(status = HttpStatusCode.OK, message = "{}")
                    },
                )
            }
        }
    }

    post("$BEHANDLING_PATH/{behandlingId}/tilleggstekst") {
        logger.debug("Mottatt post-request på '$BEHANDLING_PATH/{behandlingId}/tilleggstekst' - oppdaterer tilleggstekst for brev")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                call.withBody<OppdaterSubsumsjonBody> { body ->
                    behandlingService.oppdaterTilleggstekstBrevPåBehandling(
                        body.tilKommando(
                            behandlingId,
                            correlationId,
                            saksbehandler,
                        ),
                    ).fold(
                        {
                            when (it) {
                                is KanIkkeOppdatereTilleggstekstBrev.HarIkkeTilgang -> call.respond403Forbidden(
                                    ikkeTilgang("${it.kreverEnAvRollene} for å oppdatere tilleggstekst for brev"),
                                )

                                is KanIkkeOppdatereTilleggstekstBrev.BehandlingErSendtTilBeslutterEllerVedtatt -> call.respond400BadRequest(
                                    behandlingKanIkkeVæreSendtTilBeslutterEllerVedtatt(),
                                )
                            }
                        },
                        {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Oppdaterer tilleggstekst i brev",
                                correlationId = correlationId,
                            )

                            call.respond(status = HttpStatusCode.OK, it.toDTO())
                        },
                    )
                }
            }
        }
    }

    post("$BEHANDLING_PATH/{behandlingId}/vurderingsperiode") {
        logger.debug("Mottatt post-request på '$BEHANDLING_PATH/{behandlingId}/vurderingsperiode' - oppdaterer vurderingsperiode")
        call.withSaksbehandler(tokenService = tokenService, svarMed403HvisIngenScopes = false) { saksbehandler ->
            call.withBehandlingId { behandlingId ->
                val correlationId = call.correlationId()
                call.withBody<OppdaterVurderingsPeriodeBody> { body ->
                    behandlingService.oppdaterVurderingsPeriodeForBehandling(
                        body.tilKommando(
                            behandlingId,
                            correlationId,
                            saksbehandler,
                        ),
                    ).fold(
                        {
                            when (it) {
                                is KanIkkeOppdatereVurderingsperiode.HarIkkeTilgang -> call.respond403Forbidden(
                                    ikkeTilgang("${it.kreverEnAvRollene} for å oppdatere vurderingsperiode"),
                                )

                                is KanIkkeOppdatereVurderingsperiode.BehandlingErSendtTilBeslutterEllerVedtatt -> call.respond400BadRequest(
                                    behandlingKanIkkeVæreSendtTilBeslutterEllerVedtatt(),
                                )

                                is KanIkkeOppdatereVurderingsperiode.KanKunKrympe -> call.respond400BadRequest(
                                    nyPeriodeMåVæreInnenforVurderingsperiode("Ny periode må være innenfor opprinnelig vurderingsperiode ${it.opprinnligVurderingsperiode.tilNorskFormat()}"),
                                )
                            }
                        },
                        {
                            auditService.logMedBehandlingId(
                                behandlingId = behandlingId,
                                navIdent = saksbehandler.navIdent,
                                action = AuditLogEvent.Action.ACCESS,
                                contextMessage = "Oppdaterer vurderingsperioden",
                                correlationId = correlationId,
                            )

                            call.respond(status = HttpStatusCode.OK, it.toDTO())
                        },
                    )
                }
            }
        }
    }

    hentPersonRoute(tokenService, sakService, auditService)
    tiltakDeltagelseRoutes(behandlingService, tiltaksdeltagelseVilkårService, auditService, tokenService)
    institusjonsoppholdRoutes(behandlingService, auditService, tokenService)
    kvpRoutes(kvpVilkårService, behandlingService, auditService, tokenService)
    livsoppholdRoutes(livsoppholdVilkårService, behandlingService, auditService, tokenService)
    introRoutes(behandlingService, auditService, tokenService)
    alderRoutes(behandlingService, auditService, tokenService)
    kravfristRoutes(behandlingService, auditService, tokenService)
    stønadsdagerRoutes(behandlingService, auditService, tokenService)
}
