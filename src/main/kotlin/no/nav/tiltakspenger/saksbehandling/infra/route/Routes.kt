package no.nav.tiltakspenger.saksbehandling.infra.route

import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.rammebehandlingRoutes
import no.nav.tiltakspenger.saksbehandling.benk.infra.routes.hentBenkRoute
import no.nav.tiltakspenger.saksbehandling.infra.repo.healthRoutes
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.klagebehandlingRoutes
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
        rammebehandlingRoutes(
            behandlingService = applicationContext.behandlingContext.rammebehandlingService,
            auditService = applicationContext.personContext.auditService,
            behandleSøknadPåNyttService = applicationContext.behandlingContext.behandleSøknadPåNyttService,
            oppdaterSaksopplysningerService = applicationContext.behandlingContext.oppdaterSaksopplysningerService,
            iverksettRammebehandlingService = applicationContext.behandlingContext.iverksettRammebehandlingService,
            sendBehandlingTilBeslutningService = applicationContext.behandlingContext.sendRammebehandlingTilBeslutningService,
            forhåndsvisVedtaksbrevService = applicationContext.behandlingContext.forhåndsvisRammevedtaksbrevService,
            startRevurderingService = applicationContext.behandlingContext.startRevurderingService,
            taBehandlingService = applicationContext.behandlingContext.taRammebehandlingService,
            overtaBehandlingService = applicationContext.behandlingContext.overtaRammebehandlingService,
            leggTilbakeBehandlingService = applicationContext.behandlingContext.leggTilbakeRammebehandlingService,
            oppdaterBehandlingService = applicationContext.behandlingContext.oppdaterRammebehandlingService,
            settBehandlingPåVentService = applicationContext.behandlingContext.settRammebehandlingPåVentService,
            gjenopptaBehandlingService = applicationContext.behandlingContext.gjenopptaRammebehandlingService,
            oppdaterBeregningOgSimuleringService = applicationContext.behandlingContext.oppdaterBeregningOgSimuleringService,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
            clock = applicationContext.clock,
        )
        klagebehandlingRoutes(
            opprettKlagebehandlingService = applicationContext.klagebehandlingContext.opprettKlagebehandlingService,
            oppdaterKlagebehandlingFormkravService = applicationContext.klagebehandlingContext.oppdaterKlagebehandlingFormkravService,
            avbrytKlagebehandlingService = applicationContext.klagebehandlingContext.avbrytKlagebehandlingService,
            forhåndsvisBrevKlagebehandlingService = applicationContext.klagebehandlingContext.forhåndsvisBrevKlagebehandlingService,
            oppdaterKlagebehandlingTekstTilBrevService = applicationContext.klagebehandlingContext.oppdaterKlagebehandlingTekstTilBrevService,
            auditService = applicationContext.personContext.auditService,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
            iverksettAvvistKlagebehandlingService = applicationContext.klagebehandlingContext.iverksettAvvistKlagebehandlingService,
            vurderKlagebehandlingService = applicationContext.klagebehandlingContext.vurderKlagebehandlingService,
            opprettRammebehandlingFraKlageService = applicationContext.klagebehandlingContext.opprettRammebehandlingFraKlageService,
            overtaKlagebehandlingService = applicationContext.klagebehandlingContext.overtaKlagebehandlingService,
            taKlagebehandlingService = applicationContext.klagebehandlingContext.taKlagebehandlingService,
            leggTilbakeKlagebehandlingService = applicationContext.klagebehandlingContext.leggTilbakeKlagebehandlingService,
            settKlagebehandlingPåVentService = applicationContext.klagebehandlingContext.settKlagebehandlingPåVentService,
            gjenopptaKlagebehandlingService = applicationContext.klagebehandlingContext.gjenopptaKlagebehandlingService,
            opprettholdKlagebehandlingService = applicationContext.klagebehandlingContext.opprettholdKlagebehandlingService,
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
            mottaBrukerutfyltMeldekortService = applicationContext.mottaBrukerutfyltMeldekortService,
            underkjennMeldekortBehandlingService = applicationContext.meldekortContext.underkjennMeldekortBehandlingService,
            overtaMeldekortBehandlingService = applicationContext.meldekortContext.overtaMeldekortBehandlingService,
            taMeldekortBehandlingService = applicationContext.meldekortContext.taMeldekortBehandlingService,
            leggTilbakeMeldekortBehandlingService = applicationContext.meldekortContext.leggTilbakeMeldekortBehandlingService,
            sendMeldekortTilBeslutterService = applicationContext.meldekortContext.sendMeldekortTilBeslutterService,
            avbrytMeldekortBehandlingService = applicationContext.meldekortContext.avbrytMeldekortBehandlingService,
            clock = applicationContext.clock,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
            forhåndsvisBrevMeldekortBehandlingService = applicationContext.meldekortContext.forhåndsvisBrevMeldekortBehandlingService,
        )
        søknadRoutes(
            auditService = applicationContext.personContext.auditService,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
            startBehandlingAvManueltRegistrertSøknadService = applicationContext.søknadContext.startBehandlingAvManueltRegistrertSøknadService,
            søknadService = applicationContext.søknadContext.søknadService,
            sakService = applicationContext.sakContext.sakService,
            validerJournalpostService = applicationContext.søknadContext.validerJournalpostService,
            tiltaksdeltakerRepo = applicationContext.tiltakContext.tiltaksdeltakerRepo,
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
