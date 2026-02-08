package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev.forhåndsvisVedtaksbrevRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.iverksettRammebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn.underkjennRammebehandlingRoute
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterSimuleringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendRammebehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisRammevedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaRammebehandlingService
import java.time.Clock

fun Route.rammebehandlingRoutes(
    behandlingService: RammebehandlingService,
    auditService: AuditService,
    behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    iverksettRammebehandlingService: IverksettRammebehandlingService,
    sendBehandlingTilBeslutningService: SendRammebehandlingTilBeslutningService,
    forhåndsvisVedtaksbrevService: ForhåndsvisRammevedtaksbrevService,
    startRevurderingService: StartRevurderingService,
    taBehandlingService: TaRammebehandlingService,
    overtaBehandlingService: OvertaRammebehandlingService,
    leggTilbakeBehandlingService: LeggTilbakeRammebehandlingService,
    oppdaterBehandlingService: OppdaterRammebehandlingService,
    settBehandlingPåVentService: SettRammebehandlingPåVentService,
    gjenopptaBehandlingService: GjenopptaRammebehandlingService,
    oppdaterSimuleringService: OppdaterSimuleringService,
    tilgangskontrollService: TilgangskontrollService,
    clock: Clock,
) {
    hentRammebehandlingRoute(behandlingService, auditService, tilgangskontrollService)
    behandleSøknadPåNyttRoute(behandleSøknadPåNyttService, auditService, tilgangskontrollService)
    oppdaterSaksopplysningerRoute(auditService, oppdaterSaksopplysningerService, tilgangskontrollService)
    iverksettRammebehandlingRoute(iverksettRammebehandlingService, auditService, tilgangskontrollService)
    sendRammebehandlingTilBeslutningRoute(sendBehandlingTilBeslutningService, auditService, tilgangskontrollService)
    forhåndsvisVedtaksbrevRoute(auditService, forhåndsvisVedtaksbrevService, tilgangskontrollService)
    startRevurderingRoute(startRevurderingService, auditService, tilgangskontrollService)
    taRammebehandlingRoute(auditService, taBehandlingService, tilgangskontrollService)
    underkjennRammebehandlingRoute(auditService, behandlingService, tilgangskontrollService)
    overtaRammebehandlingRoute(overtaBehandlingService, auditService, tilgangskontrollService)
    leggTilbakeRammebehandlingRoute(auditService, leggTilbakeBehandlingService, tilgangskontrollService)
    oppdaterRammebehandlingRoute(oppdaterBehandlingService, auditService, tilgangskontrollService)
    settRammebehandlingPåVentRoute(auditService, settBehandlingPåVentService, tilgangskontrollService)
    gjenopptaRammebehandling(auditService, gjenopptaBehandlingService, tilgangskontrollService)
    oppdaterSimuleringRoute(oppdaterSimuleringService, auditService, tilgangskontrollService, clock)
}
