package no.nav.tiltakspenger.saksbehandling

import no.nav.tiltakspenger.libs.auth.test.core.EntraIdSystemtokenFakeClient
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.tilgangsstyring.TilgangsstyringServiceImpl
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.clients.person.FakeNavIdentClient
import no.nav.tiltakspenger.saksbehandling.common.DistribusjonIdGenerator
import no.nav.tiltakspenger.saksbehandling.common.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.dokument.DokumentContext
import no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenHttpClient
import no.nav.tiltakspenger.saksbehandling.fakes.clients.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.fakes.clients.FellesFakeAdressebeskyttelseKlient
import no.nav.tiltakspenger.saksbehandling.fakes.clients.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.fakes.clients.GenererFakeUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.GenererFakeVedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.JournalførFakeMeldekortGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.JournalførFakeVedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.OppgaveFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.PersonFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.PoaoTilgangskontrollFake
import no.nav.tiltakspenger.saksbehandling.fakes.clients.TiltaksdeltagelseFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.UtbetalingFakeGateway
import no.nav.tiltakspenger.saksbehandling.fakes.clients.VeilarboppfolgingFakeGateway
import no.nav.tiltakspenger.saksbehandling.meldekort.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.toSøknadstiltak
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.SakContext
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.FørstegangsbehandlingContext
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.VeilarboppfolgingGateway
import no.nav.tiltakspenger.saksbehandling.søknad.SøknadContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.UtbetalingContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.NavkontorService
import java.time.Clock

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
class LocalApplicationContext(
    usePdfGen: Boolean,
    clock: Clock,
) : ApplicationContext(gitHash = "fake-git-hash", clock = clock) {

    @Suppress("MemberVisibilityCanBePrivate")
    val journalpostIdGenerator = JournalpostIdGenerator()

    @Suppress("MemberVisibilityCanBePrivate")
    val distribusjonIdGenerator = DistribusjonIdGenerator()
    private val realPdfGen = if (usePdfGen) {
        PdfgenHttpClient(baseUrl = "http://host.docker.internal:8081")
    } else {
        null
    }

    private val utbetalingGatewayFake = UtbetalingFakeGateway()
    private val personGatewayFake = PersonFakeGateway(clock)
    private val genererFakeUtbetalingsvedtakGateway: GenererUtbetalingsvedtakGateway =
        if (usePdfGen) realPdfGen!! else GenererFakeUtbetalingsvedtakGateway()

    private val genererInnvilgelsevedtaksbrevGateway: GenererInnvilgelsesvedtaksbrevGateway =
        if (usePdfGen) realPdfGen!! else GenererFakeVedtaksbrevGateway()
    private val genererStansvedtaksbrevGateway: GenererStansvedtaksbrevGateway =
        if (usePdfGen) realPdfGen!! else GenererFakeVedtaksbrevGateway()
    private val journalførFakeMeldekortGateway = JournalførFakeMeldekortGateway(journalpostIdGenerator)
    private val journalførFakeVedtaksbrevGateway = JournalførFakeVedtaksbrevGateway(journalpostIdGenerator)
    private val dokdistFakeGateway = Dokumentdistribusjonsklient(distribusjonIdGenerator)
    private val fellesFakeAdressebeskyttelseKlient = FellesFakeAdressebeskyttelseKlient()
    private val fellesFakeSkjermingsklient = FellesFakeSkjermingsklient()
    private val poaoTilgangskontrollFake = PoaoTilgangskontrollFake()

    private val søknadId: SøknadId = SøknadId.fromString("soknad_01HSTRQBRM443VGB4WA822TE01")
    private val fnr: Fnr = Fnr.fromString("50218274152")
    private val tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelse(
        // Siden Komet eier GRUPPE_AMO, vil dette være en UUID. Hadde det vært Arena som var master ville det vært eksempelvis TA6509186.
        // Kommentar jah: Litt usikker på om Komet sender UUIDen til Arena, eller om de genererer en Arena-ID på formatet TA...
        // Kommentar Tia: Komet genererer uuid og sender denne til Arena for tiltakstyper de har tatt over. Komet er ikke master for ikke gruppeamo ennå.
        eksternTiltaksdeltagelseId = "fa287e7-ddbb-44a2-9bfa-4da4661f8b6d",
        eksternTiltaksgjennomføringsId = "5667273f-784e-4521-89c3-75b0be8ee250",
        typeKode = TiltakstypeSomGirRett.GRUPPE_AMO,
        typeNavn = "Arbeidsmarkedsoppfølging gruppe",
        fom = ObjectMother.virkningsperiode().fraOgMed,
        tom = ObjectMother.virkningsperiode().tilOgMed,
        kilde = Tiltakskilde.Komet,
    )
    private val søknadstiltak = tiltaksdeltagelse.toSøknadstiltak()

    override val oppgaveGateway: OppgaveGateway by lazy { OppgaveFakeGateway() }

    init {
        val sakRepo = SakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            saksnummerGenerator = SaksnummerGenerator.Local,
            clock = clock,
        )
        val sak = sakRepo.hentForFnr(fnr).saker.firstOrNull() ?: ObjectMother.nySak(
            fnr = fnr,
            saksnummer = sakRepo.hentNesteSaksnummer(),
        ).also { sakRepo.opprettSak(it) }
        val søknadContext = SøknadContext(sessionFactory, oppgaveGateway)
        val søknad = søknadContext.søknadRepo.hentForSøknadId(søknadId) ?: ObjectMother.nySøknad(
            fnr = fnr,
            id = søknadId,
            søknadstiltak = søknadstiltak,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            oppgaveId = ObjectMother.oppgaveId(),
        ).also { søknadContext.søknadRepo.lagre(it) }
        require(søknadstiltak == søknad.tiltak) {
            "Diff mellom søknadstiltak i lokal database og statiske tiltaksdata i LocalApplicationContext. Mulig løsning: Tøm lokal db."
        }
        leggTilPerson(
            fnr = fnr,
            personopplysningerForBruker = ObjectMother.personopplysningKjedeligFyr(fnr = fnr),
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun leggTilPerson(
        fnr: Fnr,
        personopplysningerForBruker: PersonopplysningerSøker,
    ) {
        fellesFakeSkjermingsklient.leggTil(fnr = fnr, skjermet = false)
        fellesFakeAdressebeskyttelseKlient.leggTil(fnr = fnr, gradering = listOf(AdressebeskyttelseGradering.UGRADERT))
        personGatewayFake.leggTilPersonopplysning(fnr = fnr, personopplysninger = personopplysningerForBruker)
        poaoTilgangskontrollFake.leggTil(fnr = fnr, skjermet = false)
    }

    override val entraIdSystemtokenClient = EntraIdSystemtokenFakeClient()

    override val veilarboppfolgingGateway: VeilarboppfolgingGateway by lazy {
        VeilarboppfolgingFakeGateway()
    }

    override val navkontorService: NavkontorService by lazy { NavkontorService(veilarboppfolgingGateway) }

    override val personContext =
        object : PersonContext(sessionFactory, entraIdSystemtokenClient) {
            override val personGateway = personGatewayFake
            override val tilgangsstyringService = TilgangsstyringServiceImpl(
                fellesPersonTilgangsstyringsklient = fellesFakeAdressebeskyttelseKlient,
                skjermingClient = fellesFakeSkjermingsklient,
            )
            override val poaoTilgangGateway = poaoTilgangskontrollFake
            override val navIdentClient = if (usePdfGen) FakeNavIdentClient() else super.navIdentClient
        }

    override val dokumentContext by lazy {
        object : DokumentContext(entraIdSystemtokenClient) {
            override val journalførMeldekortGateway = journalførFakeMeldekortGateway
            override val journalførVedtaksbrevGateway = journalførFakeVedtaksbrevGateway
            override val genererUtbetalingsvedtakGateway = genererFakeUtbetalingsvedtakGateway
            override val genererInnvilgelsesvedtaksbrevGateway = genererInnvilgelsevedtaksbrevGateway
        }
    }

    private val tiltakGatewayFake =
        TiltaksdeltagelseFakeGateway(søknadRepo = søknadContext.søknadRepo)

    override val tiltakContext by lazy {
        object : TiltaksdeltagelseContext(entraIdSystemtokenClient) {
            override val tiltaksdeltagelseGateway = tiltakGatewayFake
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
            clock = clock,
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
            oppgaveGateway = oppgaveGateway,
            sakRepo = sakContext.sakRepo,
            clock = clock,
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
            genererVedtaksbrevGateway = genererInnvilgelsevedtaksbrevGateway,
            genererStansvedtaksbrevGateway = genererStansvedtaksbrevGateway,
            personService = personContext.personService,
            tilgangsstyringService = personContext.tilgangsstyringService,
            dokumentdistribusjonsklient = dokdistFakeGateway,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltagelseGateway = tiltakGatewayFake,
            oppgaveGateway = oppgaveGateway,
            clock = clock,
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
            clock = clock,
        ) {
            override val utbetalingGateway = utbetalingGatewayFake
        }
    }
}
