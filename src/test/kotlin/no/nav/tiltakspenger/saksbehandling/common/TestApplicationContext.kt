package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.Bruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaFakeClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PersonRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.benk.setup.BenkOversiktContext
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokumentdistribusjonsFakeKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeKlagevedtakKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostFakeClient
import no.nav.tiltakspenger.saksbehandling.klage.infra.http.KabalClientFake
import no.nav.tiltakspenger.saksbehandling.klage.infra.setup.KlagebehandlingContext
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiFakeKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.VeilarboppfolgingFakeKlient
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.infra.http.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.saksbehandler.FakeNavIdentClient
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkContext
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.søknad.infra.setup.SøknadContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingConsumer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingFakeProducer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.setup.TiltaksdeltakelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataFakeClient

/**
 * Felles basisklasse for [TestApplicationContextMedInMemoryDb] og [TestApplicationContextMedPostgres].
 *
 * Erstatter alle eksterne klienter med fakes. Subklassene velger kun hvilken type repo som brukes:
 * - Postgres-versjonen arver alle default Postgres-repoer fra [ApplicationContext]/de respektive Context-klassene.
 * - InMemory-versjonen overstyrer `*RepoOverride`-hookene med fake-repoer.
 *
 * ## Bruk i tester
 * Foretrukket inngang er via [withTestApplicationContext] / [withTestApplicationContextAndPostgres]
 * i `TestApplicationContextEx.kt`, som setter opp Ktor-test-server og rydder opp etter seg.
 *
 * Typiske operasjoner på `tac` (= TestApplicationContext) i en test:
 * - `tac.leggTilPerson(fnr, person, tiltaksdeltakelse)` — registrer en person i alle relevante fakes
 * - `tac.leggTilBruker(token, bruker)` — knytt JWT-token til en saksbehandler/systembruker
 * - `tac.leggTilJournalpost(jpId, fnr)` — registrer en journalpost
 * - `tac.tiltaksdeltakelse()` — generer en unik tiltaksdeltakelse
 * - `tac.jwtGenerator.createJwtForSaksbehandler(...)` — lag JWT for HTTP-kall
 *
 * ## Hvordan legge til et nytt repo som InMemory må kunne overstyre
 * 1. Lag `FooFakeRepo` (implementerer `FooRepo`).
 * 2. Legg til hook i base: `protected open val fooRepoOverride: FooRepo? = null`.
 * 3. Legg til override i riktig context-blokk i base:
 *    `override val fooRepo: FooRepo get() = fooRepoOverride ?: super.fooRepo`.
 * 4. I `TestApplicationContextMedInMemoryDb`: instansier fake-repoet og sett hooken:
 *    `private val fooFakeRepo = FooFakeRepo()` og `override val fooRepoOverride = fooFakeRepo`.
 *
 * `MedPostgres` trenger ingen endring — den arver Postgres-defaulten fra Context-klassen.
 */
sealed class TestApplicationContext(
    override val clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
    protected open val idGenerators: IdGenerators,
    open val tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
) : ApplicationContext(
    gitHash = "fake-git-hash",
    clock = clock,
) {

    protected open val journalpostIdGenerator by lazy { idGenerators.journalpostIdGenerator }
    protected open val dokumentInfoIdGeneratorGenerator by lazy { idGenerators.dokumentInfoIdGeneratorSerial }
    protected open val distribusjonIdGenerator by lazy { idGenerators.distribusjonIdGenerator }

    // ====== Fake-klienter for eksterne tjenester ======
    protected open val personFakeKlient by lazy { PersonFakeKlient(clock) }
    protected open val sokosUtbetaldataFakeClient by lazy { SokosUtbetaldataFakeClient() }
    protected open val tiltakspengerArenaFakeClient by lazy { TiltakspengerArenaFakeClient() }
    protected open val genererFakeVedtaksbrevForUtbetalingKlient by lazy { GenererFakeVedtaksbrevForUtbetalingKlient() }
    protected open val genererFakeVedtaksbrevKlient by lazy { GenererFakeVedtaksbrevKlient() }
    protected open val journalførFakeMeldekortKlient by lazy { JournalførFakeMeldekortKlient(journalpostIdGenerator) }
    protected open val journalførFakeRammevedtaksbrevKlient by lazy {
        JournalførFakeRammevedtaksbrevKlient(journalpostIdGenerator)
    }
    protected open val journalførFakeKlagevedtaksbrevKlient by lazy {
        JournalførFakeKlagevedtakKlient(journalpostIdGenerator, dokumentInfoIdGeneratorGenerator)
    }
    protected open val dokumentdistribusjonsFakeKlient by lazy {
        DokumentdistribusjonsFakeKlient(distribusjonIdGenerator)
    }
    protected open val meldekortApiFakeKlient by lazy { MeldekortApiFakeKlient() }
    protected open val fellesFakeSkjermingsklient by lazy { FellesFakeSkjermingsklient() }
    protected open val fakeNavIdentClient by lazy { FakeNavIdentClient() }
    protected open val tiltaksdeltakelseFakeKlient: TiltaksdeltakelseFakeKlient by lazy {
        TiltaksdeltakelseFakeKlient { søknadContext.søknadRepo }
    }
    protected open val safJournalpostFakeClient by lazy { SafJournalpostFakeClient(clock) }
    override val safJournalpostClient by lazy { safJournalpostFakeClient }
    override val sokosUtbetaldataClient by lazy { sokosUtbetaldataFakeClient }
    override val tiltakspengerArenaClient by lazy { tiltakspengerArenaFakeClient }

    open val jwtGenerator: JwtGenerator by lazy { JwtGenerator(clock = clock) }
    override val veilarboppfolgingKlient by lazy { VeilarboppfolgingFakeKlient() }
    override val navkontorService: NavkontorService by lazy { NavkontorService(veilarboppfolgingKlient) }
    override val oppgaveKlient: OppgaveKlient by lazy { OppgaveFakeKlient() }

    protected open val kabalClientFake by lazy { KabalClientFake(clock) }

    protected open val utbetalingFakeKlient by lazy {
        UtbetalingFakeKlient(
            sakRepo = sakContext.sakRepo,
            clock = clock,
            tilbakekrevingProducer = tilbakekrevingProducer,
            skalStarteTilbakekrevinger = false,
        )
    }

    override val tilbakekrevingProducer by lazy {
        TilbakekrevingFakeProducer(
            tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
            sakRepo = sakContext.sakRepo,
            clock = clock,
        )
    }

    override val tilgangskontrollService by lazy {
        TilgangskontrollService(
            tilgangsmaskinClient = tilgangsmaskinFakeClient,
            sakService = sakContext.sakService,
        )
    }

    override val dokumentContext by lazy {
        object : DokumentContext(texasClient, clock) {
            override val dokumentdistribusjonsklient = dokumentdistribusjonsFakeKlient
            override val journalførMeldekortKlient = journalførFakeMeldekortKlient
            override val journalførRammevedtaksbrevKlient = journalførFakeRammevedtaksbrevKlient
            override val journalførKlagevedtaksbrevKlient = journalførFakeKlagevedtaksbrevKlient
            override val genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient
            override val genererVedtaksbrevForInnvilgelseKlient = genererFakeVedtaksbrevKlient
            override val genererVedtaksbrevForAvslagKlient = genererFakeVedtaksbrevKlient
            override val genererVedtaksbrevForStansKlient = genererFakeVedtaksbrevKlient
            override val genererVedtaksbrevForOpphørKlient = genererFakeVedtaksbrevKlient
            override val genererKlagebrevKlient = genererFakeVedtaksbrevKlient
        }
    }

    // ====== Repo-override-hooks ======
    // Default null = bruk repoet til den respektive Context (Postgres-bakt).
    // InMemory-subklassen overstyrer med fake-repoer.
    protected open val personRepoOverride: PersonRepo? = null
    protected open val sakRepoOverride: SakRepo? = null
    protected open val benkOversiktRepoOverride: BenkOversiktRepo? = null
    protected open val tiltaksdeltakerRepoOverride: TiltaksdeltakerRepo? = null
    protected open val statistikkRepoOverride: StatistikkRepo? = null
    protected open val søknadRepoOverride: SøknadRepo? = null
    protected open val meldekortbehandlingRepoOverride: MeldekortbehandlingRepo? = null
    protected open val meldeperiodeRepoOverride: MeldeperiodeRepo? = null
    protected open val brukersMeldekortRepoOverride: BrukersMeldekortRepo? = null
    protected open val rammevedtakRepoOverride: RammevedtakRepo? = null
    protected open val rammebehandlingRepoOverride: RammebehandlingRepo? = null
    protected open val klagebehandlingRepoOverride: KlagebehandlingRepo? = null
    protected open val klagevedtakRepoOverride: KlagevedtakRepo? = null
    protected open val meldekortvedtakRepoOverride: MeldekortvedtakRepo? = null
    protected open val utbetalingRepoOverride: UtbetalingRepo? = null
    protected open val tilbakekrevingHendelseRepoOverride: TilbakekrevingHendelseRepo? = null
    protected open val tilbakekrevingBehandlingRepoOverride: TilbakekrevingBehandlingRepo? = null

    // ====== Context-overstyringer ======
    // Eksterne klienter overstyres med fakes. Repos kommer fra repo-override-hookene over,
    // og faller tilbake til Context-ens default (Postgres-bakt) hvis hooken er null.

    override val personContext by lazy {
        object : PersonContext(sessionFactory, texasClient) {
            override val personKlient = personFakeKlient
            override val fellesSkjermingsklient = fellesFakeSkjermingsklient
            override val navIdentClient = fakeNavIdentClient
            override val personRepo: PersonRepo get() = personRepoOverride ?: super.personRepo
        }
    }

    override val statistikkContext by lazy {

        object : StatistikkContext(sessionFactory, personFakeKlient, gitHash, clock) {
            override val statistikkRepo: StatistikkRepo
                get() = statistikkRepoOverride ?: super.statistikkRepo
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
            safJournalpostClient = safJournalpostClient,
            personKlient = personContext.personKlient,
        ) {
            override val søknadRepo: SøknadRepo get() = søknadRepoOverride ?: super.søknadRepo
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
            override val tiltaksdeltakerRepo: TiltaksdeltakerRepo
                get() = tiltaksdeltakerRepoOverride ?: super.tiltaksdeltakerRepo
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
            override val sakRepo: SakRepo get() = sakRepoOverride ?: super.sakRepo
            override val benkOversiktRepo: BenkOversiktRepo
                get() = benkOversiktRepoOverride ?: super.benkOversiktRepo
        }
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
            genererVedtaksbrevForUtbetalingKlient = genererFakeVedtaksbrevForUtbetalingKlient,
            navIdentClient = personContext.navIdentClient,
            statistikkService = statistikkContext.statistikkService,
        ) {
            override val meldekortApiHttpClient = meldekortApiFakeKlient
            override val meldekortbehandlingRepo: MeldekortbehandlingRepo
                get() = meldekortbehandlingRepoOverride ?: super.meldekortbehandlingRepo
            override val meldeperiodeRepo: MeldeperiodeRepo
                get() = meldeperiodeRepoOverride ?: super.meldeperiodeRepo
            override val brukersMeldekortRepo: BrukersMeldekortRepo
                get() = brukersMeldekortRepoOverride ?: super.brukersMeldekortRepo
            override val utbetalingRepo: UtbetalingRepo
                get() = utbetalingRepoOverride ?: super.utbetalingRepo
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
        ) {
            override val rammevedtakRepo: RammevedtakRepo
                get() = rammevedtakRepoOverride ?: super.rammevedtakRepo
            override val rammebehandlingRepo: RammebehandlingRepo
                get() = rammebehandlingRepoOverride ?: super.rammebehandlingRepo
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
            override val kabalClient = kabalClientFake
            override val klagebehandlingRepo: KlagebehandlingRepo
                get() = klagebehandlingRepoOverride ?: super.klagebehandlingRepo
            override val klagevedtakRepo: KlagevedtakRepo
                get() = klagevedtakRepoOverride ?: super.klagevedtakRepo
        }
    }

    override val benkOversiktContext by lazy {

        object : BenkOversiktContext(
            sessionFactory = sessionFactory,
            tilgangskontrollService = tilgangskontrollService,
        ) {
            override val benkOversiktRepo: BenkOversiktRepo
                get() = benkOversiktRepoOverride ?: super.benkOversiktRepo
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
            override val meldekortvedtakRepo: MeldekortvedtakRepo
                get() = meldekortvedtakRepoOverride ?: super.meldekortvedtakRepo
            override val utbetalingRepo: UtbetalingRepo
                get() = utbetalingRepoOverride ?: super.utbetalingRepo
        }
    }

    override val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo
        get() = tilbakekrevingHendelseRepoOverride ?: super.tilbakekrevingHendelseRepo

    override val tilbakekrevingBehandlingRepo: TilbakekrevingBehandlingRepo
        get() = tilbakekrevingBehandlingRepoOverride ?: super.tilbakekrevingBehandlingRepo

    override val tilbakekrevingConsumer by lazy {
        TilbakekrevingConsumer(
            topic = Configuration.tilbakekrevingTopic,
            tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
            log = null,
        )
    }

    // ====== Hjelpemetoder for tester ======

    /** Genererer en ny [Tiltaksdeltakelse] med unik intern- og ekstern-id basert på [idGenerators]. */
    fun tiltaksdeltakelse(): Tiltaksdeltakelse = idGenerators.søknadstiltakIdGenerator.tiltaksdeltakelse()

    /**
     * Registrerer en person i alle nødvendige fake-klienter for at en test skal kunne behandle vedkommende:
     * - Personopplysninger ([PersonFakeKlient])
     * - Tiltaksdeltakelse ([TiltaksdeltakelseFakeKlient])
     * - Tilgangsvurdering (godkjent) ([TilgangsmaskinFakeTestClient])
     * - Tiltaksdeltaker-repo (idempotent — gjør ingenting hvis allerede registrert)
     */
    fun leggTilPerson(
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

    /**
     * Knytter et JWT-token til en gitt [Bruker] i [TexasClientFake], slik at autentiserte HTTP-kall
     * fra testen blir gjenkjent som brukeren.
     */
    fun leggTilBruker(token: String, bruker: Bruker<*, *>) {
        (texasClient as TexasClientFake).leggTilBruker(token, bruker)
    }

    /** Oppdaterer (eller fjerner med `null`) tiltaksdeltakelsen registrert i [TiltaksdeltakelseFakeKlient]. */
    fun oppdaterTiltaksdeltakelse(fnr: Fnr, tiltaksdeltakelse: Tiltaksdeltakelse?) {
        tiltaksdeltakelseFakeKlient.lagre(fnr = fnr, tiltaksdeltakelse = tiltaksdeltakelse)
    }

    /** Registrerer en journalpost i [SafJournalpostFakeClient] knyttet til gitt [fnr]. */
    fun leggTilJournalpost(journalpostId: JournalpostId, fnr: Fnr) {
        safJournalpostFakeClient.addJournalpost(journalpostId, fnr)
    }
}
