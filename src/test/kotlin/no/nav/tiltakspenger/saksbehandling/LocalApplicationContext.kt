package no.nav.tiltakspenger.saksbehandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaFakeClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeLokalClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForOpphørKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonIdGenerator
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokumentdistribusjonsFakeKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevForMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenHttpClient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.journalføring.DokumentInfoIdGeneratorRandom
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGeneratorRandom
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.journalpost.HentJournalpostDokumentService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostFakeClient
import no.nav.tiltakspenger.saksbehandling.klage.infra.http.KabalClientFake
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.LokalKabalHendelseService
import no.nav.tiltakspenger.saksbehandling.klage.infra.setup.KlagebehandlingContext
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiFakeKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiHttpClient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandlerOgBeslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.systembrukerAlleRoller
import no.nav.tiltakspenger.saksbehandling.objectmothers.toSøknadstiltak
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorFakeKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.infra.http.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.saksbehandler.FakeNavIdentClient
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingFakeProducer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.setup.TiltaksdeltakelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataFakeClient
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
    val journalpostIdGenerator = JournalpostIdGeneratorRandom()
    val dokumentInfoIdGenerator = DokumentInfoIdGeneratorRandom()

    @Suppress("MemberVisibilityCanBePrivate")
    val distribusjonIdGenerator = DistribusjonIdGenerator()
    private val realPdfGen = if (usePdfGen) {
        PdfgenHttpClient(
            baseUrl = Configuration.pdfgenUrl,
            basePdfgenrsUrl = Configuration.pdfgenrsUrl,
            isLocalOrDev = true,
        )
    } else {
        null
    }

    private val brukFakeMeldekortApi: Boolean =
        System.getenv("BRUK_FAKE_MELDEKORT_API")?.toBooleanStrictOrNull() ?: true
    private val brukFakeTexasClient: Boolean =
        System.getenv("BRUK_FAKE_AUTH")?.toBooleanStrictOrNull() ?: true

    override val texasClient =
        if (brukFakeTexasClient) TexasClientFake(clock) else super.texasClient

    private val personFakeKlient = PersonFakeKlient(clock)
    private val genererFakeVedtaksbrevForMeldekortKlient: GenererVedtaksbrevForMeldekortKlient =
        realPdfGen ?: GenererFakeVedtaksbrevForMeldekortKlient()

    private val genererFakeVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient =
        realPdfGen ?: GenererFakeVedtaksbrevKlient()
    private val genererFakeVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient =
        realPdfGen ?: GenererFakeVedtaksbrevKlient()
    private val genererFakeVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient =
        realPdfGen ?: GenererFakeVedtaksbrevKlient()
    private val genererFakeVedtaksbrevForOpphørKlient: GenererVedtaksbrevForOpphørKlient =
        realPdfGen ?: GenererFakeVedtaksbrevKlient()
    private val journalførFakeMeldekortKlient = JournalførFakeMeldekortKlient(journalpostIdGenerator)
    private val journalførFakeRammevedtaksbrevKlient = JournalførFakeRammevedtaksbrevKlient(journalpostIdGenerator)
    private val dokumentdistribusjonsklientFakeKlient = DokumentdistribusjonsFakeKlient(distribusjonIdGenerator)
    private val fellesFakeSkjermingsklient = FellesFakeSkjermingsklient()
    private val tilgangsmaskinFakeClient = TilgangsmaskinFakeLokalClient()

    private val søknadId: SøknadId = SøknadId.fromString("soknad_01HSTRQBRM443VGB4WA822TE01")
    private val fnr: Fnr = Fnr.fromString("12345678911")
    private val tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(
        // Siden Komet eier GRUPPE_AMO, vil dette være en UUID. Hadde det vært Arena som var master ville det vært eksempelvis TA6509186.
        eksternTiltaksdeltakelseId = "fa287e7-ddbb-44a2-9bfa-4da4661f8b6d",
        eksternTiltaksgjennomføringsId = "5667273f-784e-4521-89c3-75b0be8ee250",
        typeKode = TiltakstypeSomGirRettDTO.GRUPPE_AMO,
        typeNavn = "Arbeidsmarkedsoppfølging gruppe",
        fom = ObjectMother.vedtaksperiode().fraOgMed,
        tom = ObjectMother.vedtaksperiode().tilOgMed,
        kilde = Tiltakskilde.Komet,
    )
    private val søknadstiltak = tiltaksdeltakelse.toSøknadstiltak(tiltaksdeltakelse.internDeltakelseId)

    override val oppgaveKlient: OppgaveKlient by lazy { OppgaveFakeKlient() }

    override val navkontorKlient: NavkontorKlient by lazy {
        NavkontorFakeKlient()
    }

    override val tilgangskontrollService: TilgangskontrollService by lazy {
        TilgangskontrollService(
            tilgangsmaskinClient = tilgangsmaskinFakeClient,
            sakService = sakContext.sakService,
        )
    }

    override val hentJournalpostDokumentService: HentJournalpostDokumentService by lazy {
        HentJournalpostDokumentService(safJournalpostClient)
    }

    override val navkontorService: NavkontorService by lazy { NavkontorService(navkontorKlient) }

    override val personContext =
        object : PersonContext(sessionFactory, texasClient, clock) {
            override val personKlient = personFakeKlient
            override val fellesSkjermingsklient = fellesFakeSkjermingsklient
            override val navIdentClient = if (usePdfGen) FakeNavIdentClient() else super.navIdentClient
        }

    override val dokumentContext by lazy {
        object : DokumentContext(texasClient, clock) {
            override val journalførMeldekortKlient = journalførFakeMeldekortKlient
            override val journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient
            override val genererVedtaksbrevForMeldekortKlient = genererFakeVedtaksbrevForMeldekortKlient
            override val genererVedtaksbrevForInnvilgelseKlient =
                this@LocalApplicationContext.genererFakeVedtaksbrevForInnvilgelseKlient
        }
    }

    private val tiltaksdeltakelseFakeKlient by lazy {
        // TODO: Vi setter defaultTiltaksdeltakelserTilSøknadHvisDenMangler = true, som lar faken utlede tiltaksdeltakelser fra søknaden.
        // Dette er en skjør snarvei som forutsetter at søknaden allerede er persistert og at internDeltakelseId bevares (se TiltaksdeltakelseFakeKlient.hentTiltaksdeltakelseFraSøknad).
        // Den slår feil for manuelt registrerte (papir) søknader, der saksopplysningene beregnes før søknaden lagres.
        // Vurder å seede tiltaksdeltakelser eksplisitt per fnr ved oppretting av søknad i stedet.
        TiltaksdeltakelseFakeKlient(true) { søknadContext.søknadRepo }
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

    override val safJournalpostClient: SafJournalpostClient by lazy {
        SafJournalpostFakeClient(clock)
    }

    override val sokosUtbetaldataClient by lazy {
        SokosUtbetaldataFakeClient()
    }

    override val tiltakspengerArenaClient by lazy {
        TiltakspengerArenaFakeClient()
    }

    override val profile by lazy { Profile.LOCAL }

    override val sakContext by lazy {
        object : SakContext(
            sessionFactory = sessionFactory,
            personService = personContext.personService,
            fellesSkjermingsklient = personContext.fellesSkjermingsklient,
            profile = profile,
            clock = clock,
        ) {}
    }

    private val utbetalingFakeKlient by lazy {
        UtbetalingFakeKlient(
            sakRepo = sakContext.sakRepo,
            clock = clock,
            tilbakekrevingProducer = tilbakekrevingProducer,
            skalStarteTilbakekrevinger = true,
        )
    }

    override val tilbakekrevingProducer by lazy {
        TilbakekrevingFakeProducer(
            tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
            sakRepo = sakContext.sakRepo,
            clock = clock,
        )
    }

    override val meldekortContext by lazy {
        object : MeldekortContext(
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
            genererVedtaksbrevForMeldekortKlient = genererFakeVedtaksbrevForMeldekortKlient,
            navIdentClient = personContext.navIdentClient,
        ) {
            override val meldekortApiHttpClient: MeldekortApiKlient
                // Ved kjøring lokalt kan vi styre kjøring av fake eller ekte API med env-var BRUK_FAKE_MELDEKORT_API
                get() = if (brukFakeMeldekortApi) {
                    MeldekortApiFakeKlient()
                } else {
                    MeldekortApiHttpClient(
                        baseUrl = Configuration.meldekortApiUrl,
                        clock = clock,
                        authTokenProvider = object : AuthTokenProvider {
                            override suspend fun hentToken(skipCache: Boolean) =
                                texasClient.getSystemToken(
                                    Configuration.meldekortApiScope,
                                    IdentityProvider.AZUREAD,
                                )
                        },
                    )
                }
        }
    }
    override val behandlingContext by lazy {
        object : BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortbehandlingRepo = meldekortContext.meldekortbehandlingRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            statistikkService = statistikkContext.statistikkService,
            journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient,
            genererVedtaksbrevForInnvilgelseKlient = genererFakeVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererFakeVedtaksbrevForAvslagKlient,
            genererVedtaksbrevForStansKlient = genererFakeVedtaksbrevForStansKlient,
            genererVedtaksbrevForOpphørKlient = genererFakeVedtaksbrevForOpphørKlient,
            personService = personContext.personService,
            dokumentdistribusjonsklient = dokumentdistribusjonsklientFakeKlient,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltakelseKlient = tiltaksdeltakelseFakeKlient,
            clock = clock,
            sokosUtbetaldataClient = sokosUtbetaldataClient,
            navkontorService = navkontorService,
            simulerService = utbetalingContext.simulerService,
            oppgaveKlient = oppgaveKlient,
            tiltakspengerArenaClient = tiltakspengerArenaClient,
            tiltaksdeltakerRepo = tiltakContext.tiltaksdeltakerRepo,
        ) {}
    }
    override val utbetalingContext by lazy {
        object : UtbetalingContext(
            sessionFactory = sessionFactory,
            genererVedtaksbrevForMeldekortKlient = genererFakeVedtaksbrevForMeldekortKlient,
            journalførMeldekortKlient = journalførFakeMeldekortKlient,
            texasClient = texasClient,
            sakRepo = sakContext.sakRepo,
            navIdentClient = personContext.navIdentClient,
            clock = clock,
            navkontorService = navkontorService,
            statistikkService = statistikkContext.statistikkService,
        ) {
            override val utbetalingsklient = utbetalingFakeKlient
        }
    }

    override val klagebehandlingContext by lazy {
        object : KlagebehandlingContext(
            sessionFactory = sessionFactory,
            sakService = sakContext.sakService,
            clock = clock,
            validerJournalpostService = ValiderJournalpostService(safJournalpostClient, personContext.personKlient),
            hentJournalpostDokumentService = hentJournalpostDokumentService,
            personService = personContext.personService,
            navIdentClient = personContext.navIdentClient,
            genererKlagebrevKlient = dokumentContext.genererKlagebrevKlient,
            journalførKlagevedtaksbrevKlient = dokumentContext.journalførKlagevedtaksbrevKlient,
            dokumentdistribusjonsklient = dokumentdistribusjonsklientFakeKlient,
            behandleSøknadPåNyttService = behandlingContext.behandleSøknadPåNyttService,
            startRevurderingService = behandlingContext.startRevurderingService,
            taRammebehandlingService = behandlingContext.taRammebehandlingService,
            overtaRammebehandlingService = behandlingContext.overtaRammebehandlingService,
            leggTilbakeRammebehandlingService = behandlingContext.leggTilbakeRammebehandlingService,
            settRammebehandlingPåVentService = behandlingContext.settRammebehandlingPåVentService,
            gjenopptaRammebehandlingService = behandlingContext.gjenopptaRammebehandlingService,
            taMeldekortbehandlingService = meldekortContext.taMeldekortbehandlingService,
            opprettMeldekortbehandlingService = meldekortContext.opprettMeldekortbehandlingService,
            meldekortbehandlingRepo = meldekortContext.meldekortbehandlingRepo,
            overtaMeldekortbehandlingService = meldekortContext.overtaMeldekortbehandlingService,
            leggTilbakeMeldekortbehandlingService = meldekortContext.leggTilbakeMeldekortbehandlingService,
            settMeldekortbehandlingPåVentService = meldekortContext.settMeldekortbehandlingPåVentService,
            gjenopptaMeldekortbehandlingService = meldekortContext.gjenopptaMeldekortbehandlingService,
            statistikkService = statistikkContext.statistikkService,
            rammevedtakRepo = behandlingContext.rammevedtakRepo,
            meldekortvedtakRepo = utbetalingContext.meldekortvedtakRepo,
            texasClient = texasClient,
        ) {
            override val kabalClient: KabalClient = KabalClientFake(clock)
        }
    }
    val lokalKabalHendelseService = LokalKabalHendelseService(
        klagePostgresRepo = klagebehandlingContext.klagebehandlingRepo,
        klagehendelsePostgresRepo = klagebehandlingContext.klagehendelseRepo,
        clock = clock,
    )

    init {
        val sakRepo = sakContext.sakRepo
        val sak = sakRepo.hentForFnr(fnr).saker.firstOrNull() ?: ObjectMother.nySak(
            fnr = fnr,
            saksnummer = sakRepo.hentNesteSaksnummer(),
        ).also { sakRepo.opprettSak(it) }
        søknadContext.søknadRepo.hentForSøknadId(søknadId) ?: ObjectMother.nyInnvilgbarSøknad(
            fnr = fnr,
            id = søknadId,
            søknadstiltak = søknadstiltak,
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ).also {
            tiltakContext.tiltaksdeltakerRepo.lagre(
                id = it.tiltak.tiltaksdeltakerId,
                eksternId = søknadstiltak.id,
                tiltakstype = søknadstiltak.typeKode,
            )
            søknadContext.søknadRepo.lagre(it)
        }
        leggTilPerson(
            fnr = fnr,
            person = ObjectMother.personopplysningKjedeligFyr(fnr = fnr),
        )
        (texasClient as? TexasClientFake)?.also {
            it.leggTilBruker(
                TexasClientFake.LOKAL_FRONTEND_TOKEN_BRUKER_1,
                saksbehandlerOgBeslutter(
                    navIdent = "A123456",
                    brukernavn = "Sak McBeslutterface",
                ),
            )
            it.leggTilBruker(
                TexasClientFake.LOKAL_FRONTEND_TOKEN_BRUKER_2,
                saksbehandlerOgBeslutter(
                    navIdent = "B123456",
                    brukernavn = "Beslutter McSakface",
                ),
            )
            it.leggTilBruker(
                TexasClientFake.LOKAL_SYSTEMBRUKER_TOKEN,
                systembrukerAlleRoller(),
            )
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun leggTilPerson(
        fnr: Fnr,
        person: EnkelPerson,
    ) {
        fellesFakeSkjermingsklient.leggTil(fnr = fnr, skjermet = false)
        personFakeKlient.leggTilPersonopplysning(fnr = fnr, personopplysninger = person)
        tilgangsmaskinFakeClient.leggTil(fnr = fnr, harTilgang = true)
    }
}
