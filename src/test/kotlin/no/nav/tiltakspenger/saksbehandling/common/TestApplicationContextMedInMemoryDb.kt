package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.benk.setup.BenkOversiktContext
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.setup.KlagebehandlingContext
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BenkOversiktFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.person.infra.repo.PersonFakeRepo
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakFakeRepo
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkContext
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkFakeRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.setup.SøknadContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseFakeRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerFakeRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.setup.TiltaksdeltakelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakFakeRepo

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
@Suppress("UNCHECKED_CAST")
class TestApplicationContextMedInMemoryDb(
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
    override val clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
    override val texasClient: TexasClient = TexasClientFake(clock),
    override val tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    override val idGenerators: IdGenerators = IdGenerators(),
) : TestApplicationContext(
    initClock = clock,
    initIdGenerators = idGenerators,
) {
    // In-memory fake repos
    private val utbetalingFakeRepo = UtbetalingFakeRepo()
    private val rammevedtakFakeRepo = RammevedtakFakeRepo(utbetalingFakeRepo)
    private val meldekortvedtakFakeRepo = MeldekortvedtakFakeRepo(utbetalingFakeRepo)
    private val klagevedtakFakeRepo = KlagevedtakFakeRepo()
    private val statistikkFakeRepo = StatistikkFakeRepo()
    private val meldekortbehandlingFakeRepo = MeldekortbehandlingFakeRepo()
    private val meldeperiodeFakeRepo = MeldeperiodeFakeRepo()
    private val brukersMeldekortFakeRepo = BrukersMeldekortFakeRepo(meldeperiodeFakeRepo)
    private val behandlingFakeRepo = RammebehandlingFakeRepo()
    private val klagebehandlingFakeRepo = KlagebehandlingFakeRepo()
    private val søknadFakeRepo = SøknadFakeRepo(behandlingFakeRepo)
    private val tiltaksdeltakerFakeRepo = TiltaksdeltakerFakeRepo()
    private val tilbakekrevingBehandlingFakeRepo = TilbakekrevingBehandlingFakeRepo()

    private val benkOversiktFakeRepo =
        BenkOversiktFakeRepo(søknadFakeRepo, behandlingFakeRepo, meldekortbehandlingFakeRepo, klagebehandlingFakeRepo)
    private val sakFakeRepo =
        SakFakeRepo(
            behandlingRepo = behandlingFakeRepo,
            rammevedtakRepo = rammevedtakFakeRepo,
            meldekortbehandlingRepo = meldekortbehandlingFakeRepo,
            meldeperiodeRepo = meldeperiodeFakeRepo,
            meldekortvedtakRepo = meldekortvedtakFakeRepo,
            klagevedtakRepo = klagevedtakFakeRepo,
            søknadFakeRepo = søknadFakeRepo,
            klagebehandlingFakeRepo = klagebehandlingFakeRepo,
            brukersMeldekortFakeRepo = brukersMeldekortFakeRepo,
            tilbakekrevingBehandlingFakeRepo = tilbakekrevingBehandlingFakeRepo,
            clock = clock,
        )

    private val personFakeRepo =
        PersonFakeRepo(sakFakeRepo, søknadFakeRepo, meldekortbehandlingFakeRepo, behandlingFakeRepo)

    override val personContext =
        object : PersonContext(sessionFactory, texasClient) {
            override val personKlient = personFakeKlient
            override val personRepo = personFakeRepo
            override val navIdentClient = fakeNavIdentClient
        }

    override val statistikkContext by lazy {
        object : StatistikkContext(sessionFactory, personFakeKlient, gitHash, clock) {
            override val statistikkRepo = statistikkFakeRepo
        }
    }

    override val søknadContext by lazy {
        object : SøknadContext(
            sessionFactory = sessionFactory,
            rammebehandlingRepo = behandlingContext.rammebehandlingRepo,
            hentSaksopplysingerService = behandlingContext.hentSaksopplysingerService,
            sakService = sakContext.sakService,
            personService = personContext.personService,
            statistikkService = statistikkContext.statistikkService,
            clock = clock,
            safJournalpostClient = safJournalpostFakeClient,
            personKlient = personFakeKlient,
        ) {
            override val søknadRepo = søknadFakeRepo
        }
    }

    override val tiltakContext by lazy {
        object : TiltaksdeltakelseContext(
            texasClient = texasClient,
            sakService = sakContext.sakService,
            personService = personContext.personService,
            sessionFactory = sessionFactory,
        ) {
            override val tiltaksdeltakelseKlient = tiltaksdeltakelseFakeKlient
            override val tiltaksdeltakerRepo = tiltaksdeltakerFakeRepo
        }
    }

    override val sakContext by lazy {
        object : SakContext(
            sessionFactory = sessionFactory,
            personService = personContext.personService,
            fellesSkjermingsklient = fellesFakeSkjermingsklient,
            profile = Profile.LOCAL,
            clock = clock,
        ) {
            override val sakRepo = sakFakeRepo
            override val benkOversiktRepo = benkOversiktFakeRepo
        }
    }

    override val meldekortContext by lazy {
        object :
            MeldekortContext(
                sessionFactory = sessionFactory,
                sakService = sakContext.sakService,
                meldekortvedtakRepo = meldekortvedtakFakeRepo,
                texasClient = texasClient,
                navkontorService = navkontorService,
                oppgaveKlient = oppgaveKlient,
                sakRepo = sakContext.sakRepo,
                clock = clock,
                simulerService = utbetalingContext.simulerService,
                genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient,
                navIdentClient = personContext.navIdentClient,
                statistikkService = statistikkContext.statistikkService,
            ) {
            override val meldekortbehandlingRepo = meldekortbehandlingFakeRepo
            override val meldeperiodeRepo = meldeperiodeFakeRepo
            override val brukersMeldekortRepo = brukersMeldekortFakeRepo
            override val meldekortApiHttpClient = meldekortApiFakeKlient
            override val utbetalingRepo = utbetalingFakeRepo
        }
    }

    override val behandlingContext by lazy {
        object : BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortbehandlingRepo = meldekortbehandlingFakeRepo,
            meldeperiodeRepo = meldeperiodeFakeRepo,
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
            tiltaksdeltakerRepo = tiltaksdeltakerFakeRepo,
        ) {
            override val rammevedtakRepo = rammevedtakFakeRepo
            override val rammebehandlingRepo = behandlingFakeRepo
        }
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
            override val klagebehandlingRepo = klagebehandlingFakeRepo
            override val klagevedtakRepo = klagevedtakFakeRepo
            override val kabalClient = kabalClientFake
        }
    }

    override val benkOversiktContext by lazy {
        object : BenkOversiktContext(
            sessionFactory = sessionFactory,
            tilgangskontrollService = tilgangskontrollService,
        ) {
            override val benkOversiktRepo = benkOversiktFakeRepo
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
            override val meldekortvedtakRepo = meldekortvedtakFakeRepo
            override val utbetalingRepo = utbetalingFakeRepo
        }
    }

    override val tilbakekrevingHendelseRepo by lazy { TilbakekrevingHendelseFakeRepo(clock) }

    override val tilbakekrevingBehandlingRepo by lazy { tilbakekrevingBehandlingFakeRepo }
}
