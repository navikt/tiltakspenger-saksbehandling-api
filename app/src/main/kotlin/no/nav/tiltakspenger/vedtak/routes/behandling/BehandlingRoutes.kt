package no.nav.tiltakspenger.vedtak.routes.behandling

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.service.behandling.IverksettBehandlingV2Service
import no.nav.tiltakspenger.saksbehandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.saksbehandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.service.behandling.SendBehandlingTilBeslutningV2Service
import no.nav.tiltakspenger.saksbehandling.service.behandling.StartSøknadsbehandlingV2Service
import no.nav.tiltakspenger.saksbehandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.kvp.KvpVilkårService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.tiltaksdeltagelse.TiltaksdeltagelseVilkårService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.service.sak.StartRevurderingService
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.routes.behandling.beslutter.iverksettBehandlingv2Route
import no.nav.tiltakspenger.vedtak.routes.behandling.brev.forhåndsvisVedtaksbrevRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.brev.oppdaterFritekstTilVedtaksbrevRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.personopplysninger.hentPersonRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.stønadsdager.stønadsdagerRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.tilbeslutter.sendBehandlingTilBeslutterRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder.alderRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.institusjonsopphold.institusjonsoppholdRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.introduksjonsprogrammet.introRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist.kravfristRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.kvpRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold.livsoppholdRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.tiltakdeltagelse.tiltakDeltagelseRoutes

internal const val BEHANDLING_PATH = "/behandling"
internal const val BEHANDLINGER_PATH = "/behandlinger"

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tiltaksdeltagelseVilkårService: TiltaksdeltagelseVilkårService,
    tokenService: TokenService,
    sakService: SakService,
    kvpVilkårService: KvpVilkårService,
    livsoppholdVilkårService: LivsoppholdVilkårService,
    auditService: AuditService,
    søknadService: SøknadService,
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
    tiltakDeltagelseRoutes(behandlingService, tiltaksdeltagelseVilkårService, auditService, tokenService)
    institusjonsoppholdRoutes(behandlingService, auditService, tokenService)
    kvpRoutes(kvpVilkårService, behandlingService, auditService, tokenService)
    livsoppholdRoutes(livsoppholdVilkårService, behandlingService, auditService, tokenService)
    introRoutes(behandlingService, auditService, tokenService)
    alderRoutes(behandlingService, auditService, tokenService)
    kravfristRoutes(behandlingService, auditService, tokenService)
    stønadsdagerRoutes(behandlingService, auditService, tokenService)
    sendBehandlingTilBeslutterRoute(tokenService, behandlingService, auditService)
    hentBehandlingRoute(tokenService, behandlingService, auditService)
    startBehandlingRoute(tokenService, behandlingService, auditService, søknadService)
    startBehandlingV2Route(tokenService, startSøknadsbehandlingV2Service, auditService)
    oppdaterSaksopplysningerRoute(tokenService, auditService, oppdaterSaksopplysningerService)
    oppdaterBegrunnelseVilkårsvurderingRoute(tokenService, auditService, oppdaterBegrunnelseVilkårsvurderingService)
    oppdaterFritekstTilVedtaksbrevRoute(tokenService, auditService, oppdaterFritekstTilVedtaksbrevService)
    iverksettBehandlingv2Route(iverksettBehandlingV2Service, auditService, tokenService)
    sendBehandlingTilBeslutningV2Route(sendBehandlingTilBeslutningV2Service, auditService, tokenService)
    forhåndsvisVedtaksbrevRoute(tokenService, auditService, forhåndsvisVedtaksbrevService)
    startRevurderingRoute(tokenService, startRevurderingService, auditService)
    startRevurderingV2Route(tokenService, startRevurderingService, auditService)
    sendRevurderingTilBeslutningRoute(sendBehandlingTilBeslutningV2Service, auditService, tokenService)
}
