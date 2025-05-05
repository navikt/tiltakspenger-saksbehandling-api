package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.auth.core.AdRolle
import no.nav.tiltakspenger.libs.auth.core.MicrosoftEntraIdTokenService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.auth.test.core.EntraIdSystemtokenFakeClient
import no.nav.tiltakspenger.libs.auth.test.core.JwkFakeProvider
import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.saksbehandling.auth.systembrukerMapper
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.fakes.clients.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.fakes.clients.GenererFakeUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.GenererFakeVedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.JournalførFakeMeldekortGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.JournalførFakeVedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.MeldekortApiFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.OppgaveFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.PersonFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.TilgangsstyringFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.TiltaksdeltagelseFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.UtbetalingFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.VeilarboppfolgingFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.repos.BehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.BrukersMeldekortFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.MeldekortBehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.MeldeperiodeFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.PersonFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.RammevedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.SakFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.SaksoversiktFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.StatistikkSakFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.StatistikkStønadFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.SøknadFakeRepo
import no.nav.tiltakspenger.saksbehandling.fakes.repos.UtbetalingsvedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.person.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkContext
import no.nav.tiltakspenger.saksbehandling.søknad.infra.setup.SøknadContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext

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

    private val rammevedtakFakeRepo = RammevedtakFakeRepo()
    private val statistikkStønadFakeRepo = StatistikkStønadFakeRepo()
    private val statistikkSakFakeRepo = StatistikkSakFakeRepo()
    private val utbetalingGatewayFake = UtbetalingFakeGateway()
    private val meldekortBehandlingFakeRepo = MeldekortBehandlingFakeRepo()
    private val meldeperiodeFakeRepo = MeldeperiodeFakeRepo()
    private val brukersMeldekortFakeRepo = BrukersMeldekortFakeRepo(meldeperiodeFakeRepo)
    private val utbetalingsvedtakFakeRepo = UtbetalingsvedtakFakeRepo()
    private val søknadFakeRepo = SøknadFakeRepo()
    private val tiltakGatewayFake = TiltaksdeltagelseFakeGateway(søknadRepo = søknadFakeRepo)
    private val behandlingFakeRepo = BehandlingFakeRepo()
    private val personGatewayFake = PersonFakeGateway(clock)
    private val tilgangsstyringFakeGateway = TilgangsstyringFakeGateway()
    private val genererFakeMeldekortPdfGateway = GenererFakeUtbetalingsvedtakGateway()
    private val genererFakeVedtaksbrevGateway = GenererFakeVedtaksbrevGateway()
    private val journalførFakeMeldekortGateway = JournalførFakeMeldekortGateway(journalpostIdGenerator)
    private val journalførFakeVedtaksbrevGateway = JournalførFakeVedtaksbrevGateway(journalpostIdGenerator)
    private val dokdistFakeGateway = Dokumentdistribusjonsklient(distribusjonIdGenerator)
    private val meldekortApiGateway = MeldekortApiFakeGateway()

    val jwtGenerator = JwtGenerator()

    override val tokenService: TokenService = MicrosoftEntraIdTokenService(
        url = "unused",
        issuer = "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0",
        clientId = "c7adbfbb-1b1e-41f6-9b7a-af9627c04998",
        autoriserteBrukerroller = Saksbehandlerrolle.entries.map { AdRolle(it, "ROLE_${it.name}") },
        acceptIssuedAtLeeway = 0,
        acceptNotBeforeLeeway = 0,
        provider = JwkFakeProvider(jwtGenerator.jwk),
        systembrukerMapper = ::systembrukerMapper as (String, String, Set<String>) -> GenerellSystembruker<
            GenerellSystembrukerrolle,
            GenerellSystembrukerroller<GenerellSystembrukerrolle>,
            >,
    )

    fun leggTilPerson(
        fnr: Fnr,
        personopplysningerForBruker: PersonopplysningerSøker,
        tiltaksdeltagelse: Tiltaksdeltagelse,
    ) {
        personGatewayFake.leggTilPersonopplysning(fnr = fnr, personopplysninger = personopplysningerForBruker)
        tilgangsstyringFakeGateway.lagre(
            fnr = fnr,
            adressebeskyttelseGradering = listOf(AdressebeskyttelseGradering.UGRADERT),
        )
        tiltakGatewayFake.lagre(fnr = fnr, tiltaksdeltagelse = tiltaksdeltagelse)
    }

    private val saksoversiktFakeRepo =
        SaksoversiktFakeRepo(
            søknadFakeRepo = søknadFakeRepo,
            behandlingFakeRepo = behandlingFakeRepo,
        )
    private val sakFakeRepo =
        SakFakeRepo(
            behandlingRepo = behandlingFakeRepo,
            rammevedtakRepo = rammevedtakFakeRepo,
            meldekortBehandlingRepo = meldekortBehandlingFakeRepo,
            meldeperiodeRepo = meldeperiodeFakeRepo,
            utbetalingsvedtakRepo = utbetalingsvedtakFakeRepo,
            søknadFakeRepo = søknadFakeRepo,
        )

    private val personFakeRepo =
        PersonFakeRepo(sakFakeRepo, søknadFakeRepo, meldekortBehandlingFakeRepo, behandlingFakeRepo)

    override val entraIdSystemtokenClient = EntraIdSystemtokenFakeClient()

    override val veilarboppfolgingGateway = VeilarboppfolgingFakeGateway()
    override val navkontorService: NavkontorService = NavkontorService(veilarboppfolgingGateway)

    override val oppgaveGateway: OppgaveGateway = OppgaveFakeGateway()

    override val personContext =
        object : PersonContext(sessionFactory, entraIdSystemtokenClient) {
            override val personGateway = personGatewayFake
            override val personRepo = personFakeRepo
        }
    override val dokumentContext by lazy {
        object : DokumentContext(entraIdSystemtokenClient) {
            override val journalførMeldekortGateway = journalførFakeMeldekortGateway
            override val journalførVedtaksbrevGateway = journalførFakeVedtaksbrevGateway
            override val genererUtbetalingsvedtakGateway = genererFakeMeldekortPdfGateway
            override val genererInnvilgelsesvedtaksbrevGateway = genererFakeVedtaksbrevGateway
        }
    }

    override val statistikkContext by lazy {
        object : StatistikkContext(sessionFactory, tilgangsstyringFakeGateway, gitHash, clock) {
            override val statistikkStønadRepo = statistikkStønadFakeRepo
            override val statistikkSakRepo = statistikkSakFakeRepo
        }
    }

    override val søknadContext by lazy {
        object : SøknadContext(sessionFactory, oppgaveGateway) {
            override val søknadRepo = søknadFakeRepo
        }
    }

    override val tiltakContext by lazy {
        object : TiltaksdeltagelseContext(entraIdSystemtokenClient) {
            override val tiltaksdeltagelseGateway = tiltakGatewayFake
        }
    }
    override val sakContext by lazy {
        object : SakContext(
            sessionFactory = sessionFactory,
            personService = personContext.personService,
            tilgangsstyringService = tilgangsstyringFakeGateway,
            poaoTilgangGateway = personContext.poaoTilgangGateway,
            profile = Profile.LOCAL,
            clock = clock,
        ) {
            override val sakRepo = sakFakeRepo
            override val saksoversiktRepo = saksoversiktFakeRepo
        }
    }

    override val meldekortContext by lazy {
        object :
            MeldekortContext(
                sessionFactory = sessionFactory,
                sakService = sakContext.sakService,
                tilgangsstyringService = tilgangsstyringFakeGateway,
                utbetalingsvedtakRepo = utbetalingsvedtakFakeRepo,
                statistikkStønadRepo = statistikkStønadFakeRepo,
                personService = personContext.personService,
                entraIdSystemtokenClient = entraIdSystemtokenClient,
                navkontorService = navkontorService,
                oppgaveGateway = oppgaveGateway,
                sakRepo = sakContext.sakRepo,
                clock = clock,
                simulerService = utbetalingContext.simulerService,
            ) {
            override val meldekortBehandlingRepo = meldekortBehandlingFakeRepo
            override val meldeperiodeRepo = meldeperiodeFakeRepo
            override val brukersMeldekortRepo = brukersMeldekortFakeRepo
            override val meldekortApiHttpClient = meldekortApiGateway
        }
    }

    override val behandlingContext by lazy {
        object : BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortBehandlingRepo = meldekortBehandlingFakeRepo,
            meldeperiodeRepo = meldeperiodeFakeRepo,
            statistikkSakRepo = statistikkSakFakeRepo,
            statistikkStønadRepo = statistikkStønadFakeRepo,
            journalførVedtaksbrevGateway = journalførFakeVedtaksbrevGateway,
            genererVedtaksbrevGateway = genererFakeVedtaksbrevGateway,
            genererAvslagsvedtaksbrevGateway = genererFakeVedtaksbrevGateway,
            genererStansvedtaksbrevGateway = genererFakeVedtaksbrevGateway,
            personService = personContext.personService,
            tilgangsstyringService = tilgangsstyringFakeGateway,
            dokumentdistribusjonsklient = dokdistFakeGateway,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltagelseGateway = tiltakGatewayFake,
            oppgaveGateway = oppgaveGateway,
            clock = clock,
            statistikkSakService = statistikkContext.statistikkSakService,
        ) {
            override val rammevedtakRepo = rammevedtakFakeRepo
            override val behandlingRepo = behandlingFakeRepo
        }
    }

    override val utbetalingContext by lazy {
        object : UtbetalingContext(
            sessionFactory = sessionFactory,
            genererUtbetalingsvedtakGateway = genererFakeMeldekortPdfGateway,
            journalførMeldekortGateway = journalførFakeMeldekortGateway,
            entraIdSystemtokenClient = entraIdSystemtokenClient,
            navIdentClient = personContext.navIdentClient,
            sakRepo = sakContext.sakRepo,
            clock = clock,
            navkontorService = navkontorService,
        ) {
            override val utbetalingGateway = utbetalingGatewayFake
            override val utbetalingsvedtakRepo = utbetalingsvedtakFakeRepo
        }
    }
}
