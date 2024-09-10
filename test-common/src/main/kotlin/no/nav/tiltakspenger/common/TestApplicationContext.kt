package no.nav.tiltakspenger.common

import no.nav.tiltakspenger.fakes.clients.DokumentFakeGateway
import no.nav.tiltakspenger.fakes.clients.FellesFakeAdressebeskyttelseKlient
import no.nav.tiltakspenger.fakes.clients.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.fakes.clients.PersonFakeGateway
import no.nav.tiltakspenger.fakes.clients.SkjermingFakeGateway
import no.nav.tiltakspenger.fakes.clients.TiltakFakeGateway
import no.nav.tiltakspenger.fakes.clients.UtbetalingFakeGateway
import no.nav.tiltakspenger.fakes.repos.BehandlingFakeRepo
import no.nav.tiltakspenger.fakes.repos.MeldekortFakeRepo
import no.nav.tiltakspenger.fakes.repos.PersonopplysningerFakeRepo
import no.nav.tiltakspenger.fakes.repos.RammevedtakFakeRepo
import no.nav.tiltakspenger.fakes.repos.SakFakeRepo
import no.nav.tiltakspenger.fakes.repos.SaksoversiktFakeRepo
import no.nav.tiltakspenger.fakes.repos.StatistikkSakFakeRepo
import no.nav.tiltakspenger.fakes.repos.StatistikkStønadFakeRepo
import no.nav.tiltakspenger.fakes.repos.SøknadFakeRepo
import no.nav.tiltakspenger.fakes.repos.UtbetalingsvedtakFakeRepo
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.personklient.tilgangsstyring.TilgangsstyringServiceImpl
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.Personopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltak
import no.nav.tiltakspenger.vedtak.context.ApplicationContext
import no.nav.tiltakspenger.vedtak.context.DokumentContext
import no.nav.tiltakspenger.vedtak.context.FørstegangsbehandlingContext
import no.nav.tiltakspenger.vedtak.context.MeldekortContext
import no.nav.tiltakspenger.vedtak.context.PersonContext
import no.nav.tiltakspenger.vedtak.context.SakContext
import no.nav.tiltakspenger.vedtak.context.StatistikkContext
import no.nav.tiltakspenger.vedtak.context.SøknadContext
import no.nav.tiltakspenger.vedtak.context.TilgangsstyringContext
import no.nav.tiltakspenger.vedtak.context.TiltakContext
import no.nav.tiltakspenger.vedtak.context.UtbetalingContext

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
class TestApplicationContext : ApplicationContext(TestSessionFactory()) {
    val journalpostIdGenerator = JournalpostIdGenerator()

    private val rammevedtakFakeRepo = RammevedtakFakeRepo()
    private val statistikkStønadFakeRepo = StatistikkStønadFakeRepo()
    private val statistikkSakFakeRepo = StatistikkSakFakeRepo()
    private val dokumentGatewayFake = DokumentFakeGateway(journalpostIdGenerator)
    private val utbetalingGatewayFake = UtbetalingFakeGateway()
    private val meldekortFakeRepo = MeldekortFakeRepo()
    private val utbetalingsvedtakFakeRepo = UtbetalingsvedtakFakeRepo(rammevedtakFakeRepo, meldekortFakeRepo)
    private val personopplysningerFakeRepo = PersonopplysningerFakeRepo()
    private val søknadFakeRepo = SøknadFakeRepo()
    private val tiltakGatewayFake = TiltakFakeGateway()
    private val behandlingFakeRepo = BehandlingFakeRepo()
    private val skjermingFakeGateway = SkjermingFakeGateway()
    private val personGatewayFake = PersonFakeGateway()
    private val fellesPersonTilgangsstyringsklient = FellesFakeAdressebeskyttelseKlient()
    private val fellesFakeSkjermingsklient = FellesFakeSkjermingsklient()

    fun leggTilPerson(
        fnr: Fnr,
        erSkjermet: Boolean,
        personopplysningerForBruker: Personopplysninger,
        tiltak: Tiltak,
    ) {
        skjermingFakeGateway.leggTil(fnr = fnr, skjermet = erSkjermet)
        fellesFakeSkjermingsklient.leggTil(fnr = fnr, skjermet = erSkjermet)
        personGatewayFake.leggTilPersonopplysning(fnr = fnr, personopplysninger = listOf(personopplysningerForBruker))
        tiltakGatewayFake.lagre(fnr = fnr, tiltak = tiltak)
    }

    private val saksoversiktFakeRepo =
        SaksoversiktFakeRepo(
            søknadFakeRepo = søknadFakeRepo,
            behandlingFakeRepo = behandlingFakeRepo,
        )
    private val sakFakeRepo =
        SakFakeRepo(
            personopplysningerRepo = personopplysningerFakeRepo,
            behandlingRepo = behandlingFakeRepo,
            rammevedtakRepo = rammevedtakFakeRepo,
            meldekortRepo = meldekortFakeRepo,
        )

    override val personContext =
        object : PersonContext(sessionFactory) {
            override val personopplysningerRepo = personopplysningerFakeRepo
            override val personGateway = personGatewayFake
        }
    override val dokumentContext by lazy {
        object : DokumentContext() {
            override val dokumentGateway = dokumentGatewayFake
        }
    }

    override val statistikkContext by lazy {
        object : StatistikkContext(sessionFactory) {
            override val statistikkStønadRepo = statistikkStønadFakeRepo
            override val statistikkSakRepo = statistikkSakFakeRepo
        }
    }

    override val søknadContext by lazy {
        object : SøknadContext(sessionFactory) {
            override val søknadRepo = søknadFakeRepo
        }
    }
    override val tilgangsstyringContext by lazy {
        object : TilgangsstyringContext({ AccessToken("fake-access-token") }) {
            override val tilgangsstyringService =
                TilgangsstyringServiceImpl(
                    fellesPersonTilgangsstyringsklient = fellesPersonTilgangsstyringsklient,
                    skjermingClient = fellesFakeSkjermingsklient,
                )
        }
    }

    override val tiltakContext by lazy {
        object : TiltakContext() {
            override val tiltakGateway = tiltakGatewayFake
        }
    }
    override val sakContext by lazy {
        object : SakContext(
            sessionFactory = sessionFactory,
            personGateway = personGatewayFake,
            søknadService = søknadContext.søknadService,
            skjermingGateway = skjermingFakeGateway,
            statistikkSakRepo = statistikkSakFakeRepo,
            tiltakGateway = tiltakGatewayFake,
        ) {
            override val sakRepo = sakFakeRepo
            override val saksoversiktRepo = saksoversiktFakeRepo
        }
    }

    override val meldekortContext by lazy {
        object :
            MeldekortContext(sessionFactory, sakContext.sakService, tilgangsstyringContext.tilgangsstyringService) {
            override val meldekortRepo = meldekortFakeRepo
        }
    }

    override val førstegangsbehandlingContext by lazy {
        object : FørstegangsbehandlingContext(
            sessionFactory = sessionFactory,
            personopplysningRepo = personopplysningerFakeRepo,
            meldekortRepo = meldekortFakeRepo,
            sakRepo = sakFakeRepo,
            statistikkSakRepo = statistikkSakFakeRepo,
            statistikkStønadRepo = statistikkStønadFakeRepo,
        ) {
            override val rammevedtakRepo = rammevedtakFakeRepo
            override val behandlingRepo = behandlingFakeRepo
        }
    }

    override val utbetalingContext by lazy {
        object : UtbetalingContext(sessionFactory, rammevedtakFakeRepo, statistikkStønadFakeRepo, dokumentGatewayFake) {
            override val utbetalingGateway = utbetalingGatewayFake
            override val utbetalingsvedtakRepo = utbetalingsvedtakFakeRepo
        }
    }
}