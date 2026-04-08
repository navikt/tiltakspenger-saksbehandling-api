package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.benk.setup.BenkOversiktContext
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.klage.infra.setup.KlagebehandlingContext
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.setup.TiltaksdeltakelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
@Suppress("UNCHECKED_CAST")
class TestApplicationContextMedPostgres(
    override val sessionFactory: PostgresSessionFactory,
    override val clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
    override val texasClient: TexasClient = TexasClientFake(clock),
    override val tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    override val idGenerators: IdGenerators,
) : TestApplicationContext(
    initClock = clock,
    initIdGenerators = idGenerators,
) {
    override val personContext =
        object : PersonContext(sessionFactory, texasClient) {
            override val personKlient = personFakeKlient
            override val fellesSkjermingsklient = fellesFakeSkjermingsklient
            override val navIdentClient = fakeNavIdentClient
        }

    override val tiltakContext by lazy {
        object : TiltaksdeltakelseContext(
            texasClient = texasClient,
            sakService = sakContext.sakService,
            personService = personContext.personService,
            sessionFactory = sessionFactory,
        ) {
            override val tiltaksdeltakelseKlient = tiltaksdeltakelseFakeKlient
        }
    }

    override val utbetalingContext by lazy {
        object : UtbetalingContext(
            sessionFactory = sessionFactory,
            genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient,
            journalførMeldekortKlient = journalførFakeMeldekortKlient,
            texasClient = texasClient,
            navIdentClient = personContext.navIdentClient,
            sakRepo = sakContext.sakRepo,
            clock = clock,
            navkontorService = navkontorService,
            statistikkService = statistikkContext.statistikkService,
        ) {
            override val utbetalingsklient = utbetalingFakeKlient
        }
    }

    override val sakContext by lazy {
        object : SakContext(
            sessionFactory = sessionFactory,
            personService = personContext.personService,
            fellesSkjermingsklient = personContext.fellesSkjermingsklient,
            profile = Profile.LOCAL,
            clock = clock,
        ) {}
    }

    override val meldekortContext by lazy {
        object :
            MeldekortContext(
                sessionFactory = sessionFactory,
                sakService = sakContext.sakService,
                meldekortvedtakRepo = utbetalingContext.meldekortvedtakRepo,
                texasClient = texasClient,
                navkontorService = navkontorService,
                oppgaveKlient = oppgaveKlient,
                sakRepo = sakContext.sakRepo,
                clock = clock,
                simulerService = utbetalingContext.simulerService,
                statistikkService = statistikkContext.statistikkService,
                genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient,
                navIdentClient = personContext.navIdentClient,
            ) {
            override val meldekortApiHttpClient = meldekortApiFakeKlient
        }
    }

    override val behandlingContext by lazy {
        object : BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortbehandlingRepo = meldekortContext.meldekortbehandlingRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            statistikkService = statistikkContext.statistikkService,
            journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient,
            genererVedtaksbrevForInnvilgelseKlient = genererFakeVedtaksbrevKlient,
            genererVedtaksbrevForAvslagKlient = genererFakeVedtaksbrevKlient,
            genererVedtaksbrevForStansKlient = genererFakeVedtaksbrevKlient,
            genererVedtaksbrevForOpphørKlient = genererFakeVedtaksbrevKlient,
            personService = personContext.personService,
            dokumentdistribusjonsklient = dokumentdistribusjonsFakeKlient,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltakelseKlient = tiltaksdeltakelseFakeKlient,
            clock = clock,
            sokosUtbetaldataClient = sokosUtbetaldataFakeClient,
            navkontorService = navkontorService,
            simulerService = utbetalingContext.simulerService,
            oppgaveKlient = oppgaveKlient,
            tiltakspengerArenaClient = tiltakspengerArenaFakeClient,
            tiltaksdeltakerRepo = tiltakContext.tiltaksdeltakerRepo,
        ) {}
    }

    override val klagebehandlingContext by lazy {
        object : KlagebehandlingContext(
            sessionFactory = sessionFactory,
            sakService = sakContext.sakService,
            clock = clock,
            validerJournalpostService = søknadContext.validerJournalpostService,
            hentJournalpostDokumentService = hentJournalpostDokumentService,
            personService = personContext.personService,
            navIdentClient = personContext.navIdentClient,
            genererKlagebrevKlient = genererFakeVedtaksbrevKlient,
            journalførKlagevedtaksbrevKlient = journalførFakeKlagevedtaksbrevKlient,
            dokumentdistribusjonsklient = dokumentdistribusjonsFakeKlient,
            behandleSøknadPåNyttService = behandlingContext.behandleSøknadPåNyttService,
            startRevurderingService = behandlingContext.startRevurderingService,
            taRammebehandlingService = behandlingContext.taRammebehandlingService,
            overtaRammebehandlingService = behandlingContext.overtaRammebehandlingService,
            leggTilbakeRammebehandlingService = behandlingContext.leggTilbakeRammebehandlingService,
            settRammebehandlingPåVentService = behandlingContext.settRammebehandlingPåVentService,
            gjenopptaRammebehandlingService = behandlingContext.gjenopptaRammebehandlingService,
            statistikkService = statistikkContext.statistikkService,
            rammevedtakRepo = behandlingContext.rammevedtakRepo,
            texasClient = texasClient,
        ) {
            override val kabalClient = kabalClientFake
        }
    }

    override val benkOversiktContext by lazy {
        object : BenkOversiktContext(
            sessionFactory = sessionFactory,
            tilgangskontrollService = tilgangskontrollService,
        ) {}
    }
}
