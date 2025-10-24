package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaFakeClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.benk.setup.BenkOversiktContext
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonIdGenerator
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokumentdistribusjonsFakeKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiFakeKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BenkOversiktFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.VeilarboppfolgingFakeKlient
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.repo.PersonFakeRepo
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakFakeRepo
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkContext
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakFakeRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortFakeRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadFakeRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.setup.SøknadContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.setup.TiltaksdeltagelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataFakeClient

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
@Suppress("UNCHECKED_CAST")
class TestApplicationContext(
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
    override val clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
) : ApplicationContext(
    gitHash = "fake-git-hash",
    clock = clock,
) {

    @Suppress("MemberVisibilityCanBePrivate")
    val journalpostIdGenerator = JournalpostIdGenerator()

    @Suppress("MemberVisibilityCanBePrivate")
    val distribusjonIdGenerator = DistribusjonIdGenerator()

    val tilgangsmaskinFakeClient = TilgangsmaskinFakeTestClient()

    private val utbetalingFakeRepo = UtbetalingFakeRepo()
    private val rammevedtakFakeRepo = RammevedtakFakeRepo(utbetalingFakeRepo)
    private val meldekortvedtakFakeRepo = MeldekortvedtakFakeRepo(utbetalingFakeRepo)
    private val statistikkStønadFakeRepo = StatistikkStønadFakeRepo()
    private val statistikkSakFakeRepo = StatistikkSakFakeRepo()
    private val statistikkMeldekortFakeRepo = StatistikkMeldekortFakeRepo()
    private val meldekortBehandlingFakeRepo = MeldekortBehandlingFakeRepo()
    private val meldeperiodeFakeRepo = MeldeperiodeFakeRepo()
    private val brukersMeldekortFakeRepo = BrukersMeldekortFakeRepo(meldeperiodeFakeRepo)
    private val behandlingFakeRepo = BehandlingFakeRepo()
    private val søknadFakeRepo = SøknadFakeRepo(behandlingFakeRepo)
    private val tiltaksdeltagelseFakeKlient = TiltaksdeltagelseFakeKlient { søknadFakeRepo }
    private val sokosUtbetaldataFakeClient = SokosUtbetaldataFakeClient()
    private val tiltakspengerArenaFakeClient = TiltakspengerArenaFakeClient()
    private val personFakeKlient = PersonFakeKlient(clock)
    private val genererFakeVedtaksbrevForUtbetalingKlient = GenererFakeVedtaksbrevForUtbetalingKlient()
    private val genererFakseVedtaksrevForInnvilgelseKlient = GenererFakeVedtaksbrevKlient()
    private val journalførFakeMeldekortKlient = JournalførFakeMeldekortKlient(journalpostIdGenerator)
    private val journalførFakeRammevedtaksbrevKlient = JournalførFakeRammevedtaksbrevKlient(journalpostIdGenerator)
    private val dokumentdistribusjonsFakeKlient = DokumentdistribusjonsFakeKlient(distribusjonIdGenerator)
    private val meldekortApiFakeKlient = MeldekortApiFakeKlient()
    private val benkOversiktFakeRepo =
        BenkOversiktFakeRepo(søknadFakeRepo, behandlingFakeRepo, meldekortBehandlingFakeRepo)

    val jwtGenerator = JwtGenerator()

    override val texasClient = TexasClientFake()

    fun leggTilPerson(
        fnr: Fnr,
        person: EnkelPerson,
        tiltaksdeltagelse: Tiltaksdeltagelse,
    ) {
        personFakeKlient.leggTilPersonopplysning(fnr = fnr, personopplysninger = person)
        tiltaksdeltagelseFakeKlient.lagre(fnr = fnr, tiltaksdeltagelse = tiltaksdeltagelse)
        tilgangsmaskinFakeClient.leggTil(fnr, true)
    }

    fun leggTilTiltaksdeltagelse(fnr: Fnr, tiltaksdeltagelse: Tiltaksdeltagelse) {
        tiltaksdeltagelseFakeKlient.lagre(fnr = fnr, tiltaksdeltagelse = tiltaksdeltagelse)
    }

    private val saksoversiktFakeRepo =
        BenkOversiktFakeRepo(
            søknadFakeRepo = søknadFakeRepo,
            behandlingFakeRepo = behandlingFakeRepo,
            meldekortBehandlingFakeRepo = meldekortBehandlingFakeRepo,
        )
    private val sakFakeRepo =
        SakFakeRepo(
            behandlingRepo = behandlingFakeRepo,
            rammevedtakRepo = rammevedtakFakeRepo,
            meldekortBehandlingRepo = meldekortBehandlingFakeRepo,
            meldeperiodeRepo = meldeperiodeFakeRepo,
            meldekortvedtakRepo = meldekortvedtakFakeRepo,
            søknadFakeRepo = søknadFakeRepo,
        )

    private val personFakeRepo =
        PersonFakeRepo(sakFakeRepo, søknadFakeRepo, meldekortBehandlingFakeRepo, behandlingFakeRepo)

    override val veilarboppfolgingKlient = VeilarboppfolgingFakeKlient()
    override val navkontorService: NavkontorService = NavkontorService(veilarboppfolgingKlient)

    override val oppgaveKlient: OppgaveKlient = OppgaveFakeKlient()

    override val personContext =
        object : PersonContext(sessionFactory, texasClient) {
            override val personKlient = personFakeKlient
            override val personRepo = personFakeRepo
        }
    override val dokumentContext by lazy {
        object : DokumentContext(texasClient) {
            override val journalførMeldekortKlient = journalførFakeMeldekortKlient
            override val journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient
            override val genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient
            override val genererVedtaksbrevForInnvilgelseKlient = genererFakseVedtaksrevForInnvilgelseKlient
        }
    }

    override val statistikkContext by lazy {
        object : StatistikkContext(sessionFactory, personFakeKlient, gitHash, clock) {
            override val statistikkStønadRepo = statistikkStønadFakeRepo
            override val statistikkSakRepo = statistikkSakFakeRepo
            override val statistikkMeldekortRepo = statistikkMeldekortFakeRepo
        }
    }

    override val søknadContext by lazy {
        object : SøknadContext(
            sessionFactory = sessionFactory,
            behandlingRepo = behandlingContext.behandlingRepo,
            hentSaksopplysingerService = behandlingContext.hentSaksopplysingerService,
            sakService = sakContext.sakService,
            statistikkSakService = statistikkContext.statistikkSakService,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            clock = fixedClock,
        ) {
            override val søknadRepo = søknadFakeRepo
        }
    }

    override val tiltakContext by lazy {
        object : TiltaksdeltagelseContext(
            texasClient = texasClient,
            sakService = sakContext.sakService,
            personService = personContext.personService,
        ) {
            override val tiltaksdeltagelseKlient = tiltaksdeltagelseFakeKlient
        }
    }
    override val sakContext by lazy {
        object : SakContext(
            sessionFactory = sessionFactory,
            personService = personContext.personService,
            fellesSkjermingsklient = personContext.fellesSkjermingsklient,
            profile = Profile.LOCAL,
            clock = clock,
        ) {
            override val sakRepo = sakFakeRepo
            override val benkOversiktRepo = saksoversiktFakeRepo
        }
    }

    override val tilgangskontrollService = TilgangskontrollService(
        tilgangsmaskinClient = tilgangsmaskinFakeClient,
        sakService = sakContext.sakService,
    )

    private val utbetalingFakeKlient = UtbetalingFakeKlient(sakContext.sakRepo as SakFakeRepo)

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
                personKlient = personContext.personKlient,
                statistikkMeldekortRepo = statistikkContext.statistikkMeldekortRepo,
            ) {
            override val meldekortBehandlingRepo = meldekortBehandlingFakeRepo
            override val meldeperiodeRepo = meldeperiodeFakeRepo
            override val brukersMeldekortRepo = brukersMeldekortFakeRepo
            override val meldekortApiHttpClient = meldekortApiFakeKlient
            override val utbetalingRepo = utbetalingFakeRepo
        }
    }

    override val behandlingContext by lazy {
        object : BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortBehandlingRepo = meldekortBehandlingFakeRepo,
            meldeperiodeRepo = meldeperiodeFakeRepo,
            statistikkSakRepo = statistikkSakFakeRepo,
            statistikkStønadRepo = statistikkStønadFakeRepo,
            journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient,
            genererVedtaksbrevForInnvilgelseKlient = genererFakseVedtaksrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererFakseVedtaksrevForInnvilgelseKlient,
            genererVedtaksbrevForStansKlient = genererFakseVedtaksrevForInnvilgelseKlient,
            personService = personContext.personService,
            dokumentdistribusjonsklient = dokumentdistribusjonsFakeKlient,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltagelseKlient = tiltaksdeltagelseFakeKlient,
            clock = clock,
            statistikkSakService = statistikkContext.statistikkSakService,
            sokosUtbetaldataClient = sokosUtbetaldataFakeClient,
            navkontorService = navkontorService,
            simulerService = utbetalingContext.simulerService,
            personKlient = personContext.personKlient,
            oppgaveKlient = oppgaveKlient,
            tiltakspengerArenaClient = tiltakspengerArenaFakeClient,
        ) {
            override val rammevedtakRepo = rammevedtakFakeRepo
            override val behandlingRepo = behandlingFakeRepo
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
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
        ) {
            override val utbetalingsklient = utbetalingFakeKlient
            override val meldekortvedtakRepo = meldekortvedtakFakeRepo
            override val utbetalingRepo = utbetalingFakeRepo
        }
    }
}
