package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev.forhåndsvisVedtaksbrevRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.iverksettBehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn.underkjennBehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettBehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaBehandlingService

fun Route.behandlingRoutes(
    behandlingService: BehandlingService,
    tokenService: TokenService,
    auditService: AuditService,
    behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    iverksettBehandlingService: IverksettBehandlingService,
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    forhåndsvisVedtaksbrevService: ForhåndsvisVedtaksbrevService,
    startRevurderingService: StartRevurderingService,
    taBehandlingService: TaBehandlingService,
    overtaBehandlingService: OvertaBehandlingService,
    leggTilbakeBehandlingService: LeggTilbakeBehandlingService,
    oppdaterBehandlingService: OppdaterBehandlingService,
    settBehandlingPåVentService: SettBehandlingPåVentService,
    gjenopptaBehandlingService: GjenopptaBehandlingService,
) {
    hentBehandlingRoute(tokenService, behandlingService, auditService)
    behandleSøknadPåNyttRoute(tokenService, behandleSøknadPåNyttService, auditService)
    oppdaterSaksopplysningerRoute(tokenService, auditService, oppdaterSaksopplysningerService)
    iverksettBehandlingRoute(iverksettBehandlingService, auditService, tokenService)
    sendBehandlingTilBeslutningRoute(sendBehandlingTilBeslutningService, auditService, tokenService)
    forhåndsvisVedtaksbrevRoute(tokenService, auditService, forhåndsvisVedtaksbrevService)
    startRevurderingRoute(tokenService, startRevurderingService, auditService)
    taBehandlingRoute(tokenService, auditService, taBehandlingService)
    underkjennBehandlingRoute(tokenService, auditService, behandlingService)
    overtaBehandlingRoute(tokenService, overtaBehandlingService, auditService)
    leggTilbakeBehandlingRoute(tokenService, auditService, leggTilbakeBehandlingService)
    oppdaterBehandlingRoute(oppdaterBehandlingService, auditService, tokenService)
    settBehandlingPåVentRoute(tokenService, auditService, settBehandlingPåVentService)
    gjenopptaBehandling(tokenService, auditService, gjenopptaBehandlingService)
}
