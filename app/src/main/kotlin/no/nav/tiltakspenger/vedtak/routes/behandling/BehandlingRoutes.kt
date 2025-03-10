package no.nav.tiltakspenger.vedtak.routes.behandling

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.barnetillegg.oppdaterBarnetilleggRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.beslutter.iverksettBehandlingRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.brev.forhåndsvisVedtaksbrevRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.brev.oppdaterFritekstTilVedtaksbrevRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.personopplysninger.hentPersonRoute
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.StartRevurderingService

internal const val BEHANDLING_PATH = "/behandling"
internal const val BEHANDLINGER_PATH = "/behandlinger"

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tokenService: TokenService,
    sakService: SakService,
    auditService: AuditService,
    startSøknadsbehandlingService: StartSøknadsbehandlingService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    oppdaterBegrunnelseVilkårsvurderingService: OppdaterBegrunnelseVilkårsvurderingService,
    oppdaterFritekstTilVedtaksbrevService: OppdaterFritekstTilVedtaksbrevService,
    iverksettBehandlingService: IverksettBehandlingService,
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    forhåndsvisVedtaksbrevService: ForhåndsvisVedtaksbrevService,
    startRevurderingService: StartRevurderingService,
    oppdaterBarnetilleggService: OppdaterBarnetilleggService,
) {
    hentPersonRoute(tokenService, sakService, auditService)
    hentBehandlingRoute(tokenService, behandlingService, auditService)
    startBehandlingRoute(tokenService, startSøknadsbehandlingService, auditService)
    oppdaterSaksopplysningerRoute(tokenService, auditService, oppdaterSaksopplysningerService)
    oppdaterBegrunnelseVilkårsvurderingRoute(tokenService, auditService, oppdaterBegrunnelseVilkårsvurderingService)
    oppdaterFritekstTilVedtaksbrevRoute(tokenService, auditService, oppdaterFritekstTilVedtaksbrevService)
    iverksettBehandlingRoute(iverksettBehandlingService, auditService, tokenService)
    sendBehandlingTilBeslutningRoute(sendBehandlingTilBeslutningService, auditService, tokenService)
    forhåndsvisVedtaksbrevRoute(tokenService, auditService, forhåndsvisVedtaksbrevService)
    startRevurderingRoute(tokenService, startRevurderingService, auditService)
    sendRevurderingTilBeslutningRoute(sendBehandlingTilBeslutningService, auditService, tokenService)
    oppdaterBarnetilleggRoute(tokenService, auditService, oppdaterBarnetilleggService)
}
