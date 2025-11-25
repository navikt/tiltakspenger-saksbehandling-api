package no.nav.tiltakspenger.saksbehandling.infra.route

import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.behandlingRoutes
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.hentBenkRoute
import no.nav.tiltakspenger.saksbehandling.infra.repo.healthRoutes
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.meldekortRoutes
import no.nav.tiltakspenger.saksbehandling.person.infra.route.hentPersonRoute
import no.nav.tiltakspenger.saksbehandling.person.infra.route.hentPersonopplysningerBarnRoute
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.sakRoutes
import no.nav.tiltakspenger.saksbehandling.saksbehandler.route.meRoute
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.søknadRoutes
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.hentTiltakdeltakelserRoute

fun Route.routes(
    applicationContext: ApplicationContext,
    devRoutes: Route.(applicationContext: ApplicationContext) -> Unit = {},
) {
    devRoutes(applicationContext)
    healthRoutes()
    authenticate(IdentityProvider.AZUREAD.value) {
        meRoute()
        behandlingRoutes(
            behandlingService = applicationContext.behandlingContext.behandlingService,
            auditService = applicationContext.personContext.auditService,
            behandleSøknadPåNyttService = applicationContext.behandlingContext.behandleSøknadPåNyttService,
            oppdaterSaksopplysningerService = applicationContext.behandlingContext.oppdaterSaksopplysningerService,
            iverksettBehandlingService = applicationContext.behandlingContext.iverksettBehandlingService,
            sendBehandlingTilBeslutningService = applicationContext.behandlingContext.sendBehandlingTilBeslutningService,
            forhåndsvisVedtaksbrevService = applicationContext.behandlingContext.forhåndsvisVedtaksbrevService,
            startRevurderingService = applicationContext.behandlingContext.startRevurderingService,
            taBehandlingService = applicationContext.behandlingContext.taBehandlingService,
            overtaBehandlingService = applicationContext.behandlingContext.overtaBehandlingService,
            leggTilbakeBehandlingService = applicationContext.behandlingContext.leggTilbakeBehandlingService,
            oppdaterBehandlingService = applicationContext.behandlingContext.oppdaterBehandlingService,
            settBehandlingPåVentService = applicationContext.behandlingContext.settBehandlingPåVentService,
            gjenopptaBehandlingService = applicationContext.behandlingContext.gjenopptaBehandlingService,
            oppdaterSimuleringService = applicationContext.behandlingContext.oppdaterSimuleringService,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
            clock = applicationContext.clock,
        )
        hentBenkRoute(
            benkOversiktService = applicationContext.benkOversiktContext.benkOversiktService,
        )
        hentPersonRoute(
            applicationContext.sakContext.sakService,
            applicationContext.personContext.auditService,
        )
        hentPersonopplysningerBarnRoute(
            applicationContext.sakContext.sakService,
            applicationContext.personContext.auditService,
        )
        sakRoutes(
            sakService = applicationContext.sakContext.sakService,
            auditService = applicationContext.personContext.auditService,
            avbrytSøknadOgBehandlingService = applicationContext.avbrytSøknadOgBehandlingContext.avsluttSøknadOgBehandlingService,
            clock = applicationContext.clock,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
            personService = applicationContext.personContext.personService,
        )
        meldekortRoutes(
            iverksettMeldekortService = applicationContext.meldekortContext.iverksettMeldekortService,
            oppdaterMeldekortService = applicationContext.meldekortContext.oppdaterMeldekortService,
            opprettMeldekortBehandlingService = applicationContext.meldekortContext.opprettMeldekortBehandlingService,
            auditService = applicationContext.personContext.auditService,
            sakService = applicationContext.sakContext.sakService,
            mottaBrukerutfyltMeldekortService = applicationContext.mottaBrukerutfyltMeldekortService,
            underkjennMeldekortBehandlingService = applicationContext.meldekortContext.underkjennMeldekortBehandlingService,
            overtaMeldekortBehandlingService = applicationContext.meldekortContext.overtaMeldekortBehandlingService,
            taMeldekortBehandlingService = applicationContext.meldekortContext.taMeldekortBehandlingService,
            leggTilbakeMeldekortBehandlingService = applicationContext.meldekortContext.leggTilbakeMeldekortBehandlingService,
            sendMeldekortTilBeslutterService = applicationContext.meldekortContext.sendMeldekortTilBeslutterService,
            avbrytMeldekortBehandlingService = applicationContext.meldekortContext.avbrytMeldekortBehandlingService,
            clock = applicationContext.clock,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
        )
        søknadRoutes(
            auditService = applicationContext.personContext.auditService,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
            startBehandlingAvPapirsøknadService = applicationContext.søknadContext.registrerPapirsøknadService,
            søknadService = applicationContext.søknadContext.søknadService,
            sakService = applicationContext.sakContext.sakService,
            validerJournalpostService = applicationContext.søknadContext.validerJournalpostService,
        )

        hentTiltakdeltakelserRoute(
            tiltaksdeltakelseService = applicationContext.tiltakContext.tiltaksdeltakelseService,
            auditService = applicationContext.personContext.auditService,
        )
    }
    staticResources(
        remotePath = "/",
        basePackage = "static",
        index = "index.html",
        block = {
            default("index.html")
        },
    )
}
