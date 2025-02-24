package no.nav.tiltakspenger.vedtak.routes.behandling

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.service.behandling.IverksettBehandlingV2Service
import no.nav.tiltakspenger.saksbehandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.saksbehandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.service.behandling.SendBehandlingTilBeslutningV2Service
import no.nav.tiltakspenger.saksbehandling.service.behandling.StartSøknadsbehandlingV2Service
import no.nav.tiltakspenger.saksbehandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.service.sak.StartRevurderingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.beslutter.iverksettBehandlingv2Route
import no.nav.tiltakspenger.vedtak.routes.behandling.brev.forhåndsvisVedtaksbrevRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.brev.oppdaterFritekstTilVedtaksbrevRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.personopplysninger.hentPersonRoute

internal const val BEHANDLING_PATH = "/behandling"
internal const val BEHANDLINGER_PATH = "/behandlinger"

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tokenService: TokenService,
    sakService: SakService,
    auditService: AuditService,
    startSøknadsbehandlingV2Service: StartSøknadsbehandlingV2Service,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    oppdaterBegrunnelseVilkårsvurderingService: OppdaterBegrunnelseVilkårsvurderingService,
    oppdaterFritekstTilVedtaksbrevService: OppdaterFritekstTilVedtaksbrevService,
    iverksettBehandlingV2Service: IverksettBehandlingV2Service,
    sendBehandlingTilBeslutningV2Service: SendBehandlingTilBeslutningV2Service,
    forhåndsvisVedtaksbrevService: ForhåndsvisVedtaksbrevService,
    startRevurderingService: StartRevurderingService,
) {
    hentPersonRoute(tokenService, sakService, auditService)
    hentBehandlingRoute(tokenService, behandlingService, auditService)
    startBehandlingV2Route(tokenService, startSøknadsbehandlingV2Service, auditService)
    oppdaterSaksopplysningerRoute(tokenService, auditService, oppdaterSaksopplysningerService)
    oppdaterBegrunnelseVilkårsvurderingRoute(tokenService, auditService, oppdaterBegrunnelseVilkårsvurderingService)
    oppdaterFritekstTilVedtaksbrevRoute(tokenService, auditService, oppdaterFritekstTilVedtaksbrevService)
    iverksettBehandlingv2Route(iverksettBehandlingV2Service, auditService, tokenService)
    sendBehandlingTilBeslutningV2Route(sendBehandlingTilBeslutningV2Service, auditService, tokenService)
    forhåndsvisVedtaksbrevRoute(tokenService, auditService, forhåndsvisVedtaksbrevService)
    startRevurderingV2Route(tokenService, startRevurderingService, auditService)
    sendRevurderingTilBeslutningRoute(sendBehandlingTilBeslutningV2Service, auditService, tokenService)
}
