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
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.tilgangsstyring.TilgangsstyringServiceImpl
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.toSøknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltak
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltakskilde
import no.nav.tiltakspenger.vedtak.Profile
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.context.DokumentContext
import no.nav.tiltakspenger.vedtak.context.FørstegangsbehandlingContext
import no.nav.tiltakspenger.vedtak.context.MeldekortContext
import no.nav.tiltakspenger.vedtak.context.PersonContext
import no.nav.tiltakspenger.vedtak.context.SakContext
import no.nav.tiltakspenger.vedtak.context.TiltakContext
import no.nav.tiltakspenger.vedtak.context.UtbetalingContext

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
        val søknad = søknadContext.søknadRepo.hentForSøknadId(søknadId) ?: ObjectMother.nySøknad(
            fnr = fnr,
            id = søknadId,
            eksternId = tiltakId,
            søknadstiltak = søknadstiltak,
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
            override val genererVedtaksbrevGateway = genererFakeVedtaksbrevGateway
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
            søknadService = søknadContext.søknadService,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            tiltakGateway = tiltakGatewayFake,
            tilgangsstyringService = personContext.tilgangsstyringService,
            poaoTilgangGateway = personContext.poaoTilgangGateway,
            gitHash = gitHash,
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
        ) {}
    }
    override val behandlingContext by lazy {
        object : FørstegangsbehandlingContext(
            sessionFactory = sessionFactory,
            meldekortRepo = meldekortContext.meldekortRepo,
            sakRepo = sakContext.sakRepo,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            gitHash = "fake-git-hash",
            journalførVedtaksbrevGateway = journalførFakeVedtaksbrevGateway,
            genererVedtaksbrevGateway = genererFakeVedtaksbrevGateway,
            personService = personContext.personService,
            tilgangsstyringService = personContext.tilgangsstyringService,
            dokdistGateway = dokdistFakeGateway,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
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
