package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev.forhåndsvisVedtaksbrevRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.iverksettRammebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn.underkjennBehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterSimuleringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettBehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaRammebehandlingService
import java.time.Clock

fun Route.behandlingRoutes(
    behandlingService: RammebehandlingService,
    auditService: AuditService,
    behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    iverksettRammebehandlingService: IverksettRammebehandlingService,
    sendBehandlingTilBeslutningService: SendBehandlingTilBeslutningService,
    forhåndsvisVedtaksbrevService: ForhåndsvisVedtaksbrevService,
    startRevurderingService: StartRevurderingService,
    taBehandlingService: TaBehandlingService,
    overtaBehandlingService: OvertaRammebehandlingService,
    leggTilbakeBehandlingService: LeggTilbakeBehandlingService,
    oppdaterBehandlingService: OppdaterBehandlingService,
    settBehandlingPåVentService: SettBehandlingPåVentService,
    gjenopptaBehandlingService: GjenopptaBehandlingService,
    oppdaterSimuleringService: OppdaterSimuleringService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    hentBehandlingRoute(behandlingService, auditService, tilgangskontrollService)
    behandleSøknadPåNyttRoute(behandleSøknadPåNyttService, auditService, tilgangskontrollService)
    oppdaterSaksopplysningerRoute(auditService, oppdaterSaksopplysningerService, tilgangskontrollService)
    iverksettRammebehandlingRoute(iverksettRammebehandlingService, auditService, tilgangskontrollService)
    sendBehandlingTilBeslutningRoute(sendBehandlingTilBeslutningService, auditService, tilgangskontrollService)
    forhåndsvisVedtaksbrevRoute(auditService, forhåndsvisVedtaksbrevService, tilgangskontrollService)
    startRevurderingRoute(startRevurderingService, auditService, tilgangskontrollService)
    taBehandlingRoute(auditService, taBehandlingService, tilgangskontrollService)
    underkjennBehandlingRoute(auditService, behandlingService, tilgangskontrollService)
    overtaBehandlingRoute(overtaBehandlingService, auditService, tilgangskontrollService)
    leggTilbakeBehandlingRoute(auditService, leggTilbakeBehandlingService, tilgangskontrollService)
    oppdaterBehandlingRoute(oppdaterBehandlingService, auditService, tilgangskontrollService)
    settBehandlingPåVentRoute(auditService, settBehandlingPåVentService, tilgangskontrollService)
    gjenopptaBehandling(auditService, gjenopptaBehandlingService, tilgangskontrollService)
    oppdaterSimuleringRoute(oppdaterSimuleringService, auditService, tilgangskontrollService, clock)
}
