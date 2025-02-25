package no.nav.tiltakspenger.vedtak.routes

import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.routes.behandling.behandlingRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.benk.behandlingBenkRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.beslutter.behandlingBeslutterRoutes
import no.nav.tiltakspenger.vedtak.routes.meldekort.meldekortRoutes
import no.nav.tiltakspenger.vedtak.routes.sak.sakRoutes
import no.nav.tiltakspenger.vedtak.routes.saksbehandler.saksbehandlerRoutes
import no.nav.tiltakspenger.vedtak.routes.søknad.søknadRoutes

fun Route.routes(applicationContext: ApplicationContext) {
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
    )
    meldekortRoutes(
        iverksettMeldekortService = applicationContext.meldekortContext.iverksettMeldekortService,
        sendMeldekortTilBeslutterService = applicationContext.meldekortContext.sendMeldekortTilBeslutterService,
        opprettMeldekortBehandlingService = applicationContext.meldekortContext.opprettMeldekortBehandlingService,
        auditService = applicationContext.personContext.auditService,
        sakService = applicationContext.sakContext.sakService,
        tokenService = applicationContext.tokenService,
        mottaBrukerutfyltMeldekortService = applicationContext.mottaBrukerutfyltMeldekortService,
    )
    søknadRoutes(applicationContext.søknadContext.søknadService, applicationContext.sakContext.sakService, tokenService = applicationContext.tokenService)
    staticResources(
        remotePath = "/",
        basePackage = "static",
        index = "index.html",
        block = {
            default("index.html")
        },
    )
}
