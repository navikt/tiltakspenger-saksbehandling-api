package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.Bruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaFakeClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.benk.setup.BenkOversiktContext
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonIdGenerator
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokumentdistribusjonsFakeKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeKlagevedtakKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostFakeClient
import no.nav.tiltakspenger.saksbehandling.klage.infra.setup.KlagebehandlingContext
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiFakeKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.VeilarboppfolgingFakeKlient
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.infra.http.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.saksbehandler.FakeNavIdentClient
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.setup.TiltaksdeltakelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataFakeClient

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
) : TestApplicationContext(
    clock = clock,
) {
    @Suppress("MemberVisibilityCanBePrivate")
    val journalpostIdGenerator = JournalpostIdGenerator()

    @Suppress("MemberVisibilityCanBePrivate")
    val distribusjonIdGenerator = DistribusjonIdGenerator()

    private val sokosUtbetaldataFakeClient = SokosUtbetaldataFakeClient()
    private val tiltakspengerArenaFakeClient = TiltakspengerArenaFakeClient()
    private val personFakeKlient = PersonFakeKlient(clock)
    private val genererFakeVedtaksbrevForUtbetalingKlient = GenererFakeVedtaksbrevForUtbetalingKlient()
    private val genererFakeVedtaksbrevForInnvilgelseKlient = GenererFakeVedtaksbrevKlient()
    private val journalførFakeMeldekortKlient = JournalførFakeMeldekortKlient(journalpostIdGenerator)
    private val journalførFakeRammevedtaksbrevKlient = JournalførFakeRammevedtaksbrevKlient(journalpostIdGenerator)
    private val journalførFakeKlagevedtaksbrevKlient = JournalførFakeKlagevedtakKlient(journalpostIdGenerator)
    private val dokumentdistribusjonsFakeKlient = DokumentdistribusjonsFakeKlient(distribusjonIdGenerator)
    private val meldekortApiFakeKlient = MeldekortApiFakeKlient()
    private val fellesFakeSkjermingsklient = FellesFakeSkjermingsklient()
    private val fakeNavIdentClient = FakeNavIdentClient()
    override val jwtGenerator = JwtGenerator()

    override val veilarboppfolgingKlient = VeilarboppfolgingFakeKlient()
    override val navkontorService: NavkontorService = NavkontorService(veilarboppfolgingKlient)

    override val oppgaveKlient: OppgaveKlient = OppgaveFakeKlient()

    override val safJournalpostClient: SafJournalpostClient by lazy {
        SafJournalpostFakeClient(clock)
    }

    override val personContext =
        object : PersonContext(sessionFactory, texasClient) {
            override val personKlient = personFakeKlient
            override val fellesSkjermingsklient = fellesFakeSkjermingsklient
            override val navIdentClient = fakeNavIdentClient
        }
    override val dokumentContext by lazy {
        object : DokumentContext(texasClient) {
            override val journalførMeldekortKlient = journalførFakeMeldekortKlient
            override val journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient
            override val genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient
            override val genererVedtaksbrevForInnvilgelseKlient = genererFakeVedtaksbrevForInnvilgelseKlient
        }
    }

    private val tiltaksdeltakelseFakeKlient = TiltaksdeltakelseFakeKlient { søknadContext.søknadRepo }
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
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
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

    override val tilgangskontrollService = TilgangskontrollService(
        tilgangsmaskinClient = tilgangsmaskinFakeClient,
        sakService = sakContext.sakService,
    )

    private val utbetalingFakeKlient = UtbetalingFakeKlient(sakContext.sakRepo)

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
                personKlient = personContext.personKlient,
                statistikkMeldekortRepo = statistikkContext.statistikkMeldekortRepo,
                genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient,
                navIdentClient = personContext.navIdentClient,
            ) {
            override val meldekortApiHttpClient = meldekortApiFakeKlient
        }
    }

    override val behandlingContext by lazy {
        object : BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortBehandlingRepo = meldekortContext.meldekortBehandlingRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient,
            genererVedtaksbrevForInnvilgelseKlient = genererFakeVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererFakeVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForStansKlient = genererFakeVedtaksbrevForInnvilgelseKlient,
            personService = personContext.personService,
            dokumentdistribusjonsklient = dokumentdistribusjonsFakeKlient,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltakelseKlient = tiltaksdeltakelseFakeKlient,
            clock = clock,
            statistikkSakService = statistikkContext.statistikkSakService,
            sokosUtbetaldataClient = sokosUtbetaldataFakeClient,
            navkontorService = navkontorService,
            simulerService = utbetalingContext.simulerService,
            personKlient = personContext.personKlient,
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
            validerJournalpostService = ValiderJournalpostService(safJournalpostClient),
            personService = personContext.personService,
            navIdentClient = personContext.navIdentClient,
            genererKlagebrevKlient = genererFakeVedtaksbrevForInnvilgelseKlient,
            journalførKlagevedtaksbrevKlient = journalførFakeKlagevedtaksbrevKlient,
            dokumentdistribusjonsklient = dokumentdistribusjonsFakeKlient,
        ) {}
    }

    override val benkOversiktContext by lazy {
        object : BenkOversiktContext(
            sessionFactory = sessionFactory,
            tilgangskontrollService = tilgangskontrollService,
        ) {}
    }

    override fun leggTilPerson(
        fnr: Fnr,
        person: EnkelPerson,
        tiltaksdeltakelse: Tiltaksdeltakelse,
    ) {
        personFakeKlient.leggTilPersonopplysning(fnr = fnr, personopplysninger = person)
        tiltaksdeltakelseFakeKlient.lagre(fnr = fnr, tiltaksdeltakelse = tiltaksdeltakelse)
        tilgangsmaskinFakeClient.leggTil(fnr, Tilgangsvurdering.Godkjent)
        if (tiltakContext.tiltaksdeltakerRepo.hentInternId(tiltaksdeltakelse.eksternDeltakelseId) == null) {
            tiltakContext.tiltaksdeltakerRepo.lagre(
                id = tiltaksdeltakelse.internDeltakelseId,
                eksternId = tiltaksdeltakelse.eksternDeltakelseId,
                tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
            )
        }
    }

    override fun leggTilBruker(token: String, bruker: Bruker<*, *>) {
        (texasClient as TexasClientFake).leggTilBruker(token, bruker)
    }

    override fun oppdaterTiltaksdeltakelse(fnr: Fnr, tiltaksdeltakelse: Tiltaksdeltakelse?) {
        tiltaksdeltakelseFakeKlient.lagre(fnr = fnr, tiltaksdeltakelse = tiltaksdeltakelse)
    }
}
