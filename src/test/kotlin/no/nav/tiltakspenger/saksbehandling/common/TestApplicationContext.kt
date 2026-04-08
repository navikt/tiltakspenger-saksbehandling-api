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
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokumentdistribusjonsFakeKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererFakeVedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeKlagevedtakKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JournalførFakeRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostFakeClient
import no.nav.tiltakspenger.saksbehandling.klage.infra.http.KabalClientFake
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiFakeKlient
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.VeilarboppfolgingFakeKlient
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.infra.http.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import no.nav.tiltakspenger.saksbehandling.saksbehandler.FakeNavIdentClient
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingFakeProducer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataFakeClient

abstract class TestApplicationContext(
    initClock: TikkendeKlokke = TikkendeKlokke(fixedClock),
    initIdGenerators: IdGenerators,
) : ApplicationContext(
    gitHash = "fake-git-hash",
    clock = initClock,
) {
    // Properties from constructor params (safe from virtual dispatch during init)
    override val clock: TikkendeKlokke = initClock
    open val idGenerators: IdGenerators = initIdGenerators

    abstract val tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient

    // ID generator aliases
    @Suppress("MemberVisibilityCanBePrivate")
    val journalpostIdGenerator = initIdGenerators.journalpostIdGenerator
    val dokumentInfoIdGeneratorGenerator = initIdGenerators.dokumentInfoIdGeneratorSerial

    @Suppress("MemberVisibilityCanBePrivate")
    val distribusjonIdGenerator = initIdGenerators.distribusjonIdGenerator

    // Fake clients shared across test contexts
    protected val personFakeKlient = PersonFakeKlient(initClock)
    protected val sokosUtbetaldataFakeClient = SokosUtbetaldataFakeClient()
    protected val tiltakspengerArenaFakeClient = TiltakspengerArenaFakeClient()
    protected val genererFakeVedtaksbrevForUtbetalingKlient = GenererFakeVedtaksbrevForUtbetalingKlient()
    protected val genererFakeVedtaksbrevKlient = GenererFakeVedtaksbrevKlient()
    protected val journalførFakeMeldekortKlient = JournalførFakeMeldekortKlient(journalpostIdGenerator)
    protected val journalførFakeRammevedtaksbrevKlient = JournalførFakeRammevedtaksbrevKlient(journalpostIdGenerator)
    protected val journalførFakeKlagevedtaksbrevKlient =
        JournalførFakeKlagevedtakKlient(journalpostIdGenerator, dokumentInfoIdGeneratorGenerator)
    protected val dokumentdistribusjonsFakeKlient = DokumentdistribusjonsFakeKlient(distribusjonIdGenerator)
    protected val meldekortApiFakeKlient = MeldekortApiFakeKlient()
    protected val fellesFakeSkjermingsklient = FellesFakeSkjermingsklient()
    protected val fakeNavIdentClient = FakeNavIdentClient()
    protected val tiltaksdeltakelseFakeKlient = TiltaksdeltakelseFakeKlient { søknadContext.søknadRepo }

    // Override external clients with fakes
    val safJournalpostFakeClient = SafJournalpostFakeClient(initClock)
    override val safJournalpostClient = safJournalpostFakeClient
    override val sokosUtbetaldataClient = sokosUtbetaldataFakeClient
    override val tiltakspengerArenaClient = tiltakspengerArenaFakeClient

    val jwtGenerator: JwtGenerator = JwtGenerator()
    override val veilarboppfolgingKlient = VeilarboppfolgingFakeKlient()
    override val navkontorService: NavkontorService = NavkontorService(veilarboppfolgingKlient)
    override val oppgaveKlient: OppgaveKlient = OppgaveFakeKlient()

    val kabalClientFake by lazy { KabalClientFake(clock) }

    protected val utbetalingFakeKlient by lazy {
        UtbetalingFakeKlient(sakContext.sakRepo, tilbakekrevingHendelseRepo, clock)
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

    fun tiltaksdeltakelse(): Tiltaksdeltakelse {
        return idGenerators.søknadstiltakIdGenerator.tiltaksdeltakelse()
    }

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

    fun leggTilBruker(token: String, bruker: Bruker<*, *>) {
        (texasClient as TexasClientFake).leggTilBruker(token, bruker)
    }

    fun oppdaterTiltaksdeltakelse(fnr: Fnr, tiltaksdeltakelse: Tiltaksdeltakelse?) {
        tiltaksdeltakelseFakeKlient.lagre(fnr = fnr, tiltaksdeltakelse = tiltaksdeltakelse)
    }
}
