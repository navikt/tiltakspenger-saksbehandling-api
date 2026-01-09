package no.nav.tiltakspenger.saksbehandling.infra.setup

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaHttpClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinHttpClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.AvbrytSøknadOgBehandlingContext
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskSoknadsbehandlingJobb
import no.nav.tiltakspenger.saksbehandling.benk.setup.BenkOversiktContext
import no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingHttpClient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.infra.repo.DataSourceSetup
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClientImpl
import no.nav.tiltakspenger.saksbehandling.klage.infra.setup.KlagebehandlingContext
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.VeilarboppfolgingKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.VeilarboppfolgingHttpClient
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveHttpClient
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.IdenthendelseService
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb.IdenthendelseJobb
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.AktorV2Consumer
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseKafkaProducer
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.infra.setup.PersonContext
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseService
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb.PersonhendelseJobb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.LeesahConsumer
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.sak.infra.setup.SakContext
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkContext
import no.nav.tiltakspenger.saksbehandling.søknad.infra.setup.SøknadContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.TiltaksdeltakerService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena.TiltaksdeltakerArenaConsumer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.EndretTiltaksdeltakerJobb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.TiltaksdeltakerKometConsumer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaRepository
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.setup.TiltaksdeltakelseContext
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataHttpClient
import java.time.Clock

/**
 * Inneholder alle klienter, repoer og servicer.
 * Tanken er at man kan erstatte klienter og repoer med Fakes for testing.
 */
@Suppress("unused")
open class ApplicationContext(
    internal val gitHash: String,
    internal open val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    open val jdbcUrl by lazy { Configuration.database().url }
    open val dataSource by lazy { DataSourceSetup.createDatasource(jdbcUrl) }
    open val sessionCounter by lazy { SessionCounter(log) }
    open val sessionFactory: SessionFactory by lazy { PostgresSessionFactory(dataSource, sessionCounter) }

    open val texasClient: TexasClient by lazy {
        TexasHttpClient(
            introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
            tokenUrl = Configuration.naisTokenEndpoint,
            tokenExchangeUrl = Configuration.tokenExchangeEndpoint,
        )
    }

    open val tilgangsmaskinClient: TilgangsmaskinClient by lazy {
        TilgangsmaskinHttpClient(
            baseUrl = Configuration.tilgangsmaskinenUrl,
            scope = Configuration.tilgangsmaskinenScope,
            texasClient = texasClient,
        )
    }

    open val tilgangskontrollService: TilgangskontrollService by lazy {
        TilgangskontrollService(
            tilgangsmaskinClient = tilgangsmaskinClient,
            sakService = sakContext.sakService,
        )
    }

    open val veilarboppfolgingKlient: VeilarboppfolgingKlient by lazy {
        VeilarboppfolgingHttpClient(
            baseUrl = Configuration.veilarboppfolgingUrl,
            getToken = { texasClient.getSystemToken(Configuration.veilarboppfolgingScope, IdentityProvider.AZUREAD) },
        )
    }
    open val navkontorService: NavkontorService by lazy { NavkontorService(veilarboppfolgingKlient) }
    open val oppgaveKlient: OppgaveKlient by lazy {
        OppgaveHttpClient(
            baseUrl = Configuration.oppgaveUrl,
            getToken = { texasClient.getSystemToken(Configuration.oppgaveScope, IdentityProvider.AZUREAD) },
        )
    }

    open val sokosUtbetaldataClient: SokosUtbetaldataClient by lazy {
        SokosUtbetaldataHttpClient(
            baseUrl = Configuration.sokosUtbetaldataUrl,
            getToken = { texasClient.getSystemToken(Configuration.sokosUtbetaldataScope, IdentityProvider.AZUREAD) },
            clock = clock,
        )
    }

    open val tiltakspengerArenaClient: TiltakspengerArenaClient by lazy {
        TiltakspengerArenaHttpClient(
            baseUrl = Configuration.tiltakspengerArenaUrl,
            getToken = { texasClient.getSystemToken(Configuration.tiltakspengerArenaScope, IdentityProvider.AZUREAD) },
        )
    }

    open val safJournalpostClient: SafJournalpostClient by lazy {
        SafJournalpostClientImpl(
            baseUrl = Configuration.safUrl,
            getToken = {
                texasClient.getSystemToken(
                    audienceTarget = Configuration.safScope,
                    identityProvider = IdentityProvider.AZUREAD,
                    rewriteAudienceTarget = false,
                )
            },
        )
    }

    open val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository by lazy {
        TiltaksdeltakerKafkaRepository(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val tiltaksdeltakerService: TiltaksdeltakerService by lazy {
        TiltaksdeltakerService(
            tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
            søknadRepo = søknadContext.søknadRepo,
            arenaDeltakerMapper = ArenaDeltakerMapper(),
        )
    }

    open val endretTiltaksdeltakerJobb by lazy {
        EndretTiltaksdeltakerJobb(
            tiltaksdeltakerKafkaRepository = tiltaksdeltakerKafkaRepository,
            sakRepo = sakContext.sakRepo,
            oppgaveKlient = oppgaveKlient,
            behandlingRepo = behandlingContext.behandlingRepo,
            clock = clock,
        )
    }

    open val tiltaksdeltakerArenaConsumer by lazy {
        TiltaksdeltakerArenaConsumer(
            tiltaksdeltakerService = tiltaksdeltakerService,
            topic = Configuration.arenaTiltaksdeltakerTopic,
        )
    }
    open val tiltaksdeltakerKometConsumer by lazy {
        TiltaksdeltakerKometConsumer(
            tiltaksdeltakerService = tiltaksdeltakerService,
            topic = Configuration.kometTiltaksdeltakerTopic,
        )
    }

    open val personhendelseRepository: PersonhendelseRepository by lazy {
        PersonhendelseRepository(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val personhendelseService: PersonhendelseService by lazy {
        PersonhendelseService(
            sakRepo = sakContext.sakRepo,
            personhendelseRepository = personhendelseRepository,
            personKlient = personContext.personKlient,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
        )
    }

    open val identhendelseRepository: IdenthendelseRepository by lazy {
        IdenthendelseRepository(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val identhendelseService: IdenthendelseService by lazy {
        IdenthendelseService(
            sakRepo = sakContext.sakRepo,
            identhendelseRepository = identhendelseRepository,
        )
    }

    open val leesahConsumer by lazy {
        LeesahConsumer(
            topic = Configuration.leesahTopic,
            personhendelseService = personhendelseService,
        )
    }

    open val personhendelseJobb by lazy {
        PersonhendelseJobb(
            personhendelseRepository = personhendelseRepository,
            sakRepo = sakContext.sakRepo,
            oppgaveKlient = oppgaveKlient,
        )
    }

    open val aktorV2Consumer by lazy {
        AktorV2Consumer(
            topic = Configuration.aktorV2Topic,
            identhendelseService = identhendelseService,
        )
    }

    open val identhendelseKafkaProducer by lazy {
        IdenthendelseKafkaProducer(
            kafkaProducer = Producer(KafkaConfigImpl()),
            topic = Configuration.identhendelseTopic,
        )
    }

    open val identhendelseJobb by lazy {
        IdenthendelseJobb(
            identhendelseRepository = identhendelseRepository,
            identhendelseKafkaProducer = identhendelseKafkaProducer,
            sakRepo = sakContext.sakRepo,
            søknadRepo = søknadContext.søknadRepo,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            statistikkMeldekortRepo = statistikkContext.statistikkMeldekortRepo,
            sessionFactory = sessionFactory,
        )
    }

    open val delautomatiskSoknadsbehandlingJobb by lazy {
        DelautomatiskSoknadsbehandlingJobb(
            søknadRepo = søknadContext.søknadRepo,
            behandlingRepo = behandlingContext.behandlingRepo,
            startSøknadsbehandlingService = behandlingContext.startSøknadsbehandlingService,
            delautomatiskBehandlingService = behandlingContext.delautomatiskBehandlingService,
            oppdaterSaksopplysningerService = behandlingContext.oppdaterSaksopplysningerService,
        )
    }

    open val personContext by lazy { PersonContext(sessionFactory, texasClient) }
    open val dokumentContext by lazy { DokumentContext(texasClient) }
    open val statistikkContext by lazy {
        StatistikkContext(
            sessionFactory,
            personContext.personKlient,
            gitHash,
            clock,
        )
    }

    open val tiltakContext: TiltaksdeltakelseContext by lazy {
        TiltaksdeltakelseContext(
            texasClient = texasClient,
            personService = personContext.personService,
            sakService = sakContext.sakService,
            sessionFactory = sessionFactory,
        )
    }
    open val profile by lazy { Configuration.applicationProfile() }
    open val sakContext by lazy {
        SakContext(
            sessionFactory = sessionFactory,
            fellesSkjermingsklient = personContext.fellesSkjermingsklient,
            personService = personContext.personService,
            profile = profile,
            clock = clock,
        )
    }
    open val utbetalingContext by lazy {
        UtbetalingContext(
            sessionFactory = sessionFactory,
            genererVedtaksbrevForUtbetalingKlient = dokumentContext.genererVedtaksbrevForUtbetalingKlient,
            journalførMeldekortKlient = dokumentContext.journalførMeldekortKlient,
            texasClient = texasClient,
            navIdentClient = personContext.navIdentClient,
            sakRepo = sakContext.sakRepo,
            clock = clock,
            navkontorService = navkontorService,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
        )
    }
    open val meldekortContext by lazy {
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
            genererVedtaksbrevForUtbetalingKlient = dokumentContext.genererVedtaksbrevForUtbetalingKlient,
            navIdentClient = personContext.navIdentClient,

        )
    }
    open val behandlingContext by lazy {
        BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortBehandlingRepo = meldekortContext.meldekortBehandlingRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            journalførRammevedtaksbrevKlient = dokumentContext.journalførRammevedtaksbrevKlient,
            genererVedtaksbrevForInnvilgelseKlient = dokumentContext.genererVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = dokumentContext.genererVedtaksbrevForAvslagKlient,
            genererVedtaksbrevForStansKlient = dokumentContext.genererVedtaksbrevForStansKlient,
            dokumentdistribusjonsklient = dokumentContext.dokumentdistribusjonsklient,
            personService = personContext.personService,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltakelseKlient = tiltakContext.tiltaksdeltakelseKlient,
            clock = clock,
            statistikkSakService = statistikkContext.statistikkSakService,
            sokosUtbetaldataClient = sokosUtbetaldataClient,
            navkontorService = navkontorService,
            simulerService = utbetalingContext.simulerService,
            personKlient = personContext.personKlient,
            oppgaveKlient = oppgaveKlient,
            tiltakspengerArenaClient = tiltakspengerArenaClient,
            tiltaksdeltakerRepo = tiltakContext.tiltaksdeltakerRepo,
        )
    }

    open val klagebehandlingContext by lazy {
        KlagebehandlingContext(
            sessionFactory = sessionFactory,
            sakService = sakContext.sakService,
            clock = clock,
            validerJournalpostService = søknadContext.validerJournalpostService,
        )
    }

    open val søknadContext by lazy {
        SøknadContext(
            sessionFactory = sessionFactory,
            behandlingRepo = behandlingContext.behandlingRepo,
            hentSaksopplysingerService = behandlingContext.hentSaksopplysingerService,
            sakService = sakContext.sakService,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkSakService = statistikkContext.statistikkSakService,
            clock = clock,
            safJournalpostClient = safJournalpostClient,
        )
    }

    open val benkOversiktContext by lazy {
        BenkOversiktContext(
            sessionFactory = sessionFactory,
            tilgangskontrollService = tilgangskontrollService,
        )
    }

    private val datadelingKlient by lazy {
        DatadelingHttpClient(
            baseUrl = Configuration.datadelingUrl,
            getToken = { texasClient.getSystemToken(Configuration.datadelingScope, IdentityProvider.AZUREAD) },
        )
    }

    val sendTilDatadelingService by lazy {
        SendTilDatadelingService(
            rammevedtakRepo = behandlingContext.rammevedtakRepo,
            behandlingRepo = behandlingContext.behandlingRepo,
            sakRepo = sakContext.sakRepo,
            meldekortBehandlingRepo = meldekortContext.meldekortBehandlingRepo,
            meldekortvedtakRepo = utbetalingContext.meldekortvedtakRepo,
            datadelingClient = datadelingKlient,
            clock = clock,
        )
    }

    val mottaBrukerutfyltMeldekortService by lazy {
        MottaBrukerutfyltMeldekortService(
            brukersMeldekortRepo = meldekortContext.brukersMeldekortRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            sakService = sakContext.sakService,
        )
    }

    val avbrytSøknadOgBehandlingContext by lazy {
        AvbrytSøknadOgBehandlingContext(
            sakService = sakContext.sakService,
            søknadService = søknadContext.søknadService,
            behandlingService = behandlingContext.behandlingService,
            statistikkSakService = statistikkContext.statistikkSakService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }
}
