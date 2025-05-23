package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.oppdaterBarnetilleggRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev.forhåndsvisVedtaksbrevRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev.oppdaterFritekstTilVedtaksbrevRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.iverksettBehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn.underkjennBehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.StartRevurderingService

internal const val BEHANDLING_PATH = "/behandling"
internal const val BEHANDLINGER_PATH = "/behandlinger"

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tokenService: TokenService,
    auditService: AuditService,
    startSøknadsbehandlingService: StartSøknadsbehandlingService,
    behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    oppdaterBegrunnelseVilkårsvurderingService: OppdaterBegrunnelseVilkårsvurderingService,
    oppdaterFritekstTilVedtaksbrevService: OppdaterFritekstTilVedtaksbrevService,
    iverksettBehandlingService: IverksettBehandlingService,
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    forhåndsvisVedtaksbrevService: ForhåndsvisVedtaksbrevService,
    startRevurderingService: StartRevurderingService,
    oppdaterBarnetilleggService: OppdaterBarnetilleggService,
    taBehandlingService: TaBehandlingService,
    overtaBehandlingService: OvertaBehandlingService,
    leggTilbakeBehandlingService: LeggTilbakeBehandlingService,
) {
    hentBehandlingRoute(tokenService, behandlingService, auditService)
    startSøknadsbehandlingRoute(tokenService, startSøknadsbehandlingService, auditService)
    behandleSøknadPåNyttRoute(tokenService, behandleSøknadPåNyttService, auditService)
    oppdaterSaksopplysningerRoute(tokenService, auditService, oppdaterSaksopplysningerService)
    oppdaterBegrunnelseVilkårsvurderingRoute(tokenService, auditService, oppdaterBegrunnelseVilkårsvurderingService)
    oppdaterFritekstTilVedtaksbrevRoute(tokenService, auditService, oppdaterFritekstTilVedtaksbrevService)
    iverksettBehandlingRoute(iverksettBehandlingService, auditService, tokenService)
    sendSøknadsbehandlingTilBeslutningRoute(sendBehandlingTilBeslutningService, auditService, tokenService)
    forhåndsvisVedtaksbrevRoute(tokenService, auditService, forhåndsvisVedtaksbrevService)
    startRevurderingRoute(tokenService, startRevurderingService, auditService)
    sendRevurderingTilBeslutningRoute(sendBehandlingTilBeslutningService, auditService, tokenService)
    oppdaterBarnetilleggRoute(tokenService, auditService, oppdaterBarnetilleggService)
    taBehandlingRoute(tokenService, auditService, taBehandlingService)
    underkjennBehandlingRoute(tokenService, auditService, behandlingService)
    overtaBehandlingRoute(tokenService, overtaBehandlingService, auditService)
    leggTilbakeBehandlingRoute(tokenService, auditService, leggTilbakeBehandlingService)
}
