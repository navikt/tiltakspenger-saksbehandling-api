package no.nav.tiltakspenger.saksbehandling.routes

import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.context.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.behandling.behandlingRoutes
import no.nav.tiltakspenger.saksbehandling.routes.behandling.benk.behandlingBenkRoutes
import no.nav.tiltakspenger.saksbehandling.routes.behandling.beslutter.behandlingBeslutterRoutes
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.meldekortRoutes
import no.nav.tiltakspenger.saksbehandling.routes.sak.sakRoutes
import no.nav.tiltakspenger.saksbehandling.routes.saksbehandler.saksbehandlerRoutes
import no.nav.tiltakspenger.saksbehandling.routes.søknad.søknadRoutes

fun Route.routes(
    applicationContext: ApplicationContext,
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    devRoutes(applicationContext)
    healthRoutes()
    saksbehandlerRoutes(applicationContext.tokenService)
    behandlingRoutes(
        behandlingService = applicationContext.behandlingContext.behandlingService,
        sakService = applicationContext.sakContext.sakService,
        auditService = applicationContext.personContext.auditService,
        tokenService = applicationContext.tokenService,
        startSøknadsbehandlingService = applicationContext.behandlingContext.startSøknadsbehandlingService,
        oppdaterSaksopplysningerService = applicationContext.behandlingContext.oppdaterSaksopplysningerService,
        oppdaterBegrunnelseVilkårsvurderingService = applicationContext.behandlingContext.oppdaterBegrunnelseVilkårsvurderingService,
        oppdaterFritekstTilVedtaksbrevService = applicationContext.behandlingContext.oppdaterFritekstTilVedtaksbrevService,
        iverksettBehandlingService = applicationContext.behandlingContext.iverksettBehandlingService,
        sendBehandlingTilBeslutningService = applicationContext.behandlingContext.sendBehandlingTilBeslutningService,
        forhåndsvisVedtaksbrevService = applicationContext.behandlingContext.forhåndsvisVedtaksbrevService,
        startRevurderingService = applicationContext.behandlingContext.startRevurderingService,
        oppdaterBarnetilleggService = applicationContext.behandlingContext.oppdaterBarnetilleggService,
    )
    behandlingBenkRoutes(
        tokenService = applicationContext.tokenService,
        behandlingService = applicationContext.behandlingContext.behandlingService,
        sakService = applicationContext.sakContext.sakService,
        auditService = applicationContext.personContext.auditService,
    )
    behandlingBeslutterRoutes(
        tokenService = applicationContext.tokenService,
        behandlingService = applicationContext.behandlingContext.behandlingService,
        auditService = applicationContext.personContext.auditService,
    )
    sakRoutes(
        tokenService = applicationContext.tokenService,
        sakService = applicationContext.sakContext.sakService,
        auditService = applicationContext.personContext.auditService,
        avbrytSøknadOgBehandlingService = applicationContext.avbrytSøknadOgBehandlingContext.avsluttSøknadOgBehandlingService,
    )
    meldekortRoutes(
        iverksettMeldekortService = applicationContext.meldekortContext.iverksettMeldekortService,
        sendMeldekortTilBeslutterService = applicationContext.meldekortContext.sendMeldekortTilBeslutterService,
        opprettMeldekortBehandlingService = applicationContext.meldekortContext.opprettMeldekortBehandlingService,
        opprettMeldekortKorrigeringService = applicationContext.meldekortContext.opprettMeldekortKorrigeringService,
        auditService = applicationContext.personContext.auditService,
        sakService = applicationContext.sakContext.sakService,
        tokenService = applicationContext.tokenService,
        mottaBrukerutfyltMeldekortService = applicationContext.mottaBrukerutfyltMeldekortService,
    )
    søknadRoutes(
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
