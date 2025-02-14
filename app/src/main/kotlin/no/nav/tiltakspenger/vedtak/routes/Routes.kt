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
        tiltaksdeltagelseVilkårService = applicationContext.behandlingContext.tiltaksdeltagelseVilkårService,
        sakService = applicationContext.sakContext.sakService,
        kvpVilkårService = applicationContext.behandlingContext.kvpVilkårService,
        livsoppholdVilkårService = applicationContext.behandlingContext.livsoppholdVilkårService,
        auditService = applicationContext.personContext.auditService,
        tokenService = applicationContext.tokenService,
        søknadService = applicationContext.søknadContext.søknadService,
        startSøknadsbehandlingV2Service = applicationContext.behandlingContext.startSøknadsbehandlingV2Service,
        oppdaterSaksopplysningerService = applicationContext.behandlingContext.oppdaterSaksopplysningerService,
        oppdaterBegrunnelseVilkårsvurderingService = applicationContext.behandlingContext.oppdaterBegrunnelseVilkårsvurderingService,
        oppdaterFritekstTilVedtaksbrevService = applicationContext.behandlingContext.oppdaterFritekstTilVedtaksbrevService,
    )
    behandlingBenkRoutes(
        tokenService = applicationContext.tokenService,
        behandlingService = applicationContext.behandlingContext.behandlingService,
        sakService = applicationContext.sakContext.sakService,
        auditService = applicationContext.personContext.auditService,
        startRevurderingService = applicationContext.behandlingContext.startRevurderingService,
        søknadService = applicationContext.søknadContext.søknadService,
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
