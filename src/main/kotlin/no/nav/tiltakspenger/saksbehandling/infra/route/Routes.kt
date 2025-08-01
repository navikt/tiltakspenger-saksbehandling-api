package no.nav.tiltakspenger.saksbehandling.infra.route

import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.behandlingRoutes
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.hentBenkRoute
import no.nav.tiltakspenger.saksbehandling.infra.repo.healthRoutes
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.meldekortRoutes
import no.nav.tiltakspenger.saksbehandling.person.infra.route.hentPersonRoute
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.sakRoutes
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.meRoute
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.mottaSøknadRoute

fun Route.routes(
    applicationContext: ApplicationContext,
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    devRoutes(applicationContext)
    healthRoutes()
    meRoute(applicationContext.tokenService)
    behandlingRoutes(
        behandlingService = applicationContext.behandlingContext.behandlingService,
        auditService = applicationContext.personContext.auditService,
        tokenService = applicationContext.tokenService,
        behandleSøknadPåNyttService = applicationContext.behandlingContext.behandleSøknadPåNyttService,
        oppdaterSaksopplysningerService = applicationContext.behandlingContext.oppdaterSaksopplysningerService,
        oppdaterBegrunnelseVilkårsvurderingService = applicationContext.behandlingContext.oppdaterBegrunnelseVilkårsvurderingService,
        oppdaterFritekstTilVedtaksbrevService = applicationContext.behandlingContext.oppdaterFritekstTilVedtaksbrevService,
        iverksettBehandlingService = applicationContext.behandlingContext.iverksettBehandlingService,
        sendBehandlingTilBeslutningService = applicationContext.behandlingContext.sendBehandlingTilBeslutningService,
        forhåndsvisVedtaksbrevService = applicationContext.behandlingContext.forhåndsvisVedtaksbrevService,
        startRevurderingService = applicationContext.behandlingContext.startRevurderingService,
        oppdaterBarnetilleggService = applicationContext.behandlingContext.oppdaterBarnetilleggService,
        taBehandlingService = applicationContext.behandlingContext.taBehandlingService,
        overtaBehandlingService = applicationContext.behandlingContext.overtaBehandlingService,
        leggTilbakeBehandlingService = applicationContext.behandlingContext.leggTilbakeBehandlingService,
        oppdaterBehandlingService = applicationContext.behandlingContext.oppdaterBehandlingService,
    )
    hentBenkRoute(
        tokenService = applicationContext.tokenService,
        benkOversiktService = applicationContext.benkOversiktContext.benkOversiktService,
    )
    hentPersonRoute(
        applicationContext.tokenService,
        applicationContext.sakContext.sakService,
        applicationContext.personContext.auditService,
    )
    sakRoutes(
        tokenService = applicationContext.tokenService,
        sakService = applicationContext.sakContext.sakService,
        auditService = applicationContext.personContext.auditService,
        avbrytSøknadOgBehandlingService = applicationContext.avbrytSøknadOgBehandlingContext.avsluttSøknadOgBehandlingService,
        clock = applicationContext.clock,
    )
    meldekortRoutes(
        iverksettMeldekortService = applicationContext.meldekortContext.iverksettMeldekortService,
        oppdaterMeldekortService = applicationContext.meldekortContext.oppdaterMeldekortService,
        opprettMeldekortBehandlingService = applicationContext.meldekortContext.opprettMeldekortBehandlingService,
        auditService = applicationContext.personContext.auditService,
        sakService = applicationContext.sakContext.sakService,
        tokenService = applicationContext.tokenService,
        mottaBrukerutfyltMeldekortService = applicationContext.mottaBrukerutfyltMeldekortService,
        underkjennMeldekortBehandlingService = applicationContext.meldekortContext.underkjennMeldekortBehandlingService,
        overtaMeldekortBehandlingService = applicationContext.meldekortContext.overtaMeldekortBehandlingService,
        taMeldekortBehandlingService = applicationContext.meldekortContext.taMeldekortBehandlingService,
        leggTilbakeMeldekortBehandlingService = applicationContext.meldekortContext.leggTilbakeMeldekortBehandlingService,
        sendMeldekortTilBeslutterService = applicationContext.meldekortContext.sendMeldekortTilBeslutterService,
        avbrytMeldekortBehandlingService = applicationContext.meldekortContext.avbrytMeldekortBehandlingService,
        clock = applicationContext.clock,
    )
    mottaSøknadRoute(
        applicationContext.søknadContext.søknadService,
        applicationContext.sakContext.sakService,
        tokenService = applicationContext.tokenService,
    )
    staticResources(
        remotePath = "/",
        basePackage = "static",
        index = "index.html",
        block = {
            default("index.html")
        },
    )
}
