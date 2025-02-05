package no.nav.tiltakspenger

import no.nav.tiltakspenger.common.DistribusjonIdGenerator
import no.nav.tiltakspenger.common.JournalpostIdGenerator
import no.nav.tiltakspenger.fakes.clients.DokdistFakeGateway
import no.nav.tiltakspenger.fakes.clients.FellesFakeAdressebeskyttelseKlient
import no.nav.tiltakspenger.fakes.clients.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.fakes.clients.GenererFakeUtbetalingsvedtakGateway
import no.nav.tiltakspenger.fakes.clients.GenererFakeVedtaksbrevGateway
import no.nav.tiltakspenger.fakes.clients.JournalførFakeMeldekortGateway
import no.nav.tiltakspenger.fakes.clients.JournalførFakeVedtaksbrevGateway
import no.nav.tiltakspenger.fakes.clients.PersonFakeGateway
import no.nav.tiltakspenger.fakes.clients.PoaoTilgangskontrollFake
import no.nav.tiltakspenger.fakes.clients.TiltakFakeGateway
import no.nav.tiltakspenger.fakes.clients.UtbetalingFakeGateway
import no.nav.tiltakspenger.felles.TiltakId
import no.nav.tiltakspenger.libs.auth.test.core.EntraIdSystemtokenFakeClient
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.tilgangsstyring.TilgangsstyringServiceImpl
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.toSøknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.domene.sak.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltak
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.ports.VeilarboppfolgingGateway
import no.nav.tiltakspenger.utbetaling.service.NavkontorService
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.Profile
import no.nav.tiltakspenger.vedtak.clients.oppgave.OppgaveHttpClient
import no.nav.tiltakspenger.vedtak.clients.veilarboppfolging.VeilarboppfolgingHttpClient
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.context.DokumentContext
import no.nav.tiltakspenger.vedtak.context.FørstegangsbehandlingContext
import no.nav.tiltakspenger.vedtak.context.MeldekortContext
import no.nav.tiltakspenger.vedtak.context.PersonContext
import no.nav.tiltakspenger.vedtak.context.SakContext
import no.nav.tiltakspenger.vedtak.context.SøknadContext
import no.nav.tiltakspenger.vedtak.context.TiltakContext
import no.nav.tiltakspenger.vedtak.context.UtbetalingContext
import no.nav.tiltakspenger.vedtak.repository.sak.SakPostgresRepo

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
class LocalApplicationContext : ApplicationContext(gitHash = "fake-git-hash") {

    val journalpostIdGenerator = JournalpostIdGenerator()
    val distribusjonIdGenerator = DistribusjonIdGenerator()

    private val utbetalingGatewayFake = UtbetalingFakeGateway()
    private val tiltakGatewayFake = TiltakFakeGateway()
    private val personGatewayFake = PersonFakeGateway()
    private val genererFakeUtbetalingsvedtakGateway = GenererFakeUtbetalingsvedtakGateway()
    private val genererFakeVedtaksbrevGateway = GenererFakeVedtaksbrevGateway()
    private val journalførFakeMeldekortGateway = JournalførFakeMeldekortGateway(journalpostIdGenerator)
    private val journalførFakeVedtaksbrevGateway = JournalførFakeVedtaksbrevGateway(journalpostIdGenerator)
    private val dokdistFakeGateway = DokdistFakeGateway(distribusjonIdGenerator)
    private val fellesFakeAdressebeskyttelseKlient = FellesFakeAdressebeskyttelseKlient()
    private val fellesFakeSkjermingsklient = FellesFakeSkjermingsklient()
    private val poaoTilgangskontrollFake = PoaoTilgangskontrollFake()

    private val søknadId: SøknadId = SøknadId.fromString("soknad_01HSTRQBRM443VGB4WA822TE01")
    private val fnr: Fnr = Fnr.fromString("50218274152")
    private val tiltakId: TiltakId = TiltakId.fromString("tilt_01JETND3NDGHE0YHWFTVAN93B0")
    private val tiltak: Tiltak = ObjectMother.tiltak(
        id = tiltakId,
        // Siden Komet eier GRUPPE_AMO, vil dette være en UUID. Hadde det vært Arena som var master ville det vært eksempelvis TA6509186.
        // Kommentar jah: Litt usikker på om Komet sender UUIDen til Arena, eller om de genererer en Arena-ID på formatet TA...
        // Kommentar Tia: Komet genererer uuid og sender denne til Arena for tiltakstyper de har tatt over. Komet er ikke master for ikke gruppeamo ennå.
        eksternTiltaksdeltagelseId = "fa287e7-ddbb-44a2-9bfa-4da4661f8b6d",
        eksternTiltaksgjennomføringsId = "5667273f-784e-4521-89c3-75b0be8ee250",
        typeKode = TiltakstypeSomGirRett.GRUPPE_AMO,
        typeNavn = "Arbeidsmarkedsoppfølging gruppe",
        fom = ObjectMother.vurderingsperiode().fraOgMed,
        tom = ObjectMother.vurderingsperiode().tilOgMed,
        kilde = Tiltakskilde.Komet,
    )
    private val søknadstiltak = tiltak.toSøknadstiltak()

    init {
        val sakRepo = SakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            saksnummerGenerator = SaksnummerGenerator.Local,
        )
        val sak = sakRepo.hentForFnr(fnr).saker.firstOrNull() ?: ObjectMother.nySak(
            fnr = fnr,
            saksnummer = sakRepo.hentNesteSaksnummer(),
        ).also { sakRepo.opprettSak(it) }
        val oppgaveGateway = OppgaveHttpClient(
            baseUrl = Configuration.oppgaveUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.oppgaveScope) },
        )
        val søknadContext = SøknadContext(sessionFactory, oppgaveGateway)
        val søknad = søknadContext.søknadRepo.hentForSøknadId(søknadId) ?: ObjectMother.nySøknad(
            fnr = fnr,
            id = søknadId,
            eksternId = tiltakId,
            søknadstiltak = søknadstiltak,
            sak = sak,
            oppgaveId = ObjectMother.oppgaveId(),
        ).also { søknadContext.søknadRepo.lagre(it) }
        require(søknadstiltak == søknad.tiltak) {
            "Diff mellom søknadstiltak i lokal database og statiske tiltaksdata i LocalApplicationContext. Mulig løsning: Tøm lokal db."
        }
        leggTilPerson(
            fnr = fnr,
            personopplysningerForBruker = ObjectMother.personopplysningKjedeligFyr(fnr = fnr),
            tiltak = tiltak,
        )
    }

    fun leggTilPerson(
        fnr: Fnr,
        personopplysningerForBruker: PersonopplysningerSøker,
        tiltak: Tiltak,
    ) {
        fellesFakeSkjermingsklient.leggTil(fnr = fnr, skjermet = false)
        fellesFakeAdressebeskyttelseKlient.leggTil(fnr = fnr, gradering = listOf(AdressebeskyttelseGradering.UGRADERT))
        personGatewayFake.leggTilPersonopplysning(fnr = fnr, personopplysninger = personopplysningerForBruker)
        tiltakGatewayFake.lagre(fnr = fnr, tiltak = tiltak)
        poaoTilgangskontrollFake.leggTil(fnr = fnr, skjermet = false)
    }

    override val entraIdSystemtokenClient = EntraIdSystemtokenFakeClient()

    override val veilarboppfolgingGateway: VeilarboppfolgingGateway by lazy {
        VeilarboppfolgingHttpClient(
            baseUrl = Configuration.veilarboppfolgingUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.veilarboppfolgingScope) },
        )
    }
    override val navkontorService: NavkontorService by lazy { NavkontorService(veilarboppfolgingGateway) }

    override val oppgaveGateway: OppgaveGateway by lazy {
        OppgaveHttpClient(
            baseUrl = Configuration.oppgaveUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.oppgaveScope) },
        )
    }

    override val personContext =
        object : PersonContext(sessionFactory, entraIdSystemtokenClient) {
            override val personGateway = personGatewayFake
            override val tilgangsstyringService = TilgangsstyringServiceImpl(
                fellesPersonTilgangsstyringsklient = fellesFakeAdressebeskyttelseKlient,
                skjermingClient = fellesFakeSkjermingsklient,
            )
            override val poaoTilgangGateway = poaoTilgangskontrollFake
        }
    override val dokumentContext by lazy {
        object : DokumentContext(entraIdSystemtokenClient) {
            override val journalførMeldekortGateway = journalførFakeMeldekortGateway
            override val journalførVedtaksbrevGateway = journalførFakeVedtaksbrevGateway
            override val genererUtbetalingsvedtakGateway = genererFakeUtbetalingsvedtakGateway
            override val genererInnvilgelsesvedtaksbrevGateway = genererFakeVedtaksbrevGateway
        }
    }

    override val tiltakContext by lazy {
        object : TiltakContext(entraIdSystemtokenClient) {
            override val tiltakGateway = tiltakGatewayFake
        }
    }
    override val profile by lazy { Profile.LOCAL }

    override val sakContext by lazy {
        object : SakContext(
            sessionFactory = sessionFactory,
            personService = personContext.personService,
            tilgangsstyringService = personContext.tilgangsstyringService,
            poaoTilgangGateway = personContext.poaoTilgangGateway,
            profile = profile,
        ) {}
    }
    override val meldekortContext by lazy {
        object : MeldekortContext(
            sessionFactory = sessionFactory,
            sakService = sakContext.sakService,
            tilgangsstyringService = personContext.tilgangsstyringService,
            utbetalingsvedtakRepo = utbetalingContext.utbetalingsvedtakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            personService = personContext.personService,
            entraIdSystemtokenClient = entraIdSystemtokenClient,
            navkontorService = navkontorService,
        ) {}
    }
    override val behandlingContext by lazy {
        object : FørstegangsbehandlingContext(
            sessionFactory = sessionFactory,
            meldekortBehandlingRepo = meldekortContext.meldekortBehandlingRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            gitHash = "fake-git-hash",
            journalførVedtaksbrevGateway = journalførFakeVedtaksbrevGateway,
            genererVedtaksbrevGateway = genererFakeVedtaksbrevGateway,
            genererStansvedtaksbrevGateway = genererFakeVedtaksbrevGateway,
            personService = personContext.personService,
            tilgangsstyringService = personContext.tilgangsstyringService,
            dokdistGateway = dokdistFakeGateway,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltakGateway = tiltakGatewayFake,
            oppgaveGateway = oppgaveGateway,
            søknadRepo = søknadContext.søknadRepo,
        ) {}
    }
    override val utbetalingContext by lazy {
        object : UtbetalingContext(
            sessionFactory = sessionFactory,
            genererUtbetalingsvedtakGateway = genererFakeUtbetalingsvedtakGateway,
            journalførMeldekortGateway = journalførFakeMeldekortGateway,
            entraIdSystemtokenClient = entraIdSystemtokenClient,
            sakRepo = sakContext.sakRepo,
            navIdentClient = personContext.navIdentClient,
        ) {
            override val utbetalingGateway = utbetalingGatewayFake
        }
    }
}
