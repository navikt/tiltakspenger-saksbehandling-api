package no.nav.tiltakspenger.saksbehandling.infra.setup

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenHttpClient
import no.nav.tiltakspenger.libs.auth.core.MicrosoftEntraIdTokenService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.saksbehandling.auth.systembrukerMapper
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.AvbrytSøknadOgBehandlingContext
import no.nav.tiltakspenger.saksbehandling.behandling.infra.setup.BehandlingOgVedtakContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingHttpClient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.setup.DokumentContext
import no.nav.tiltakspenger.saksbehandling.infra.repo.DataSourceSetup
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup.MeldekortContext
import no.nav.tiltakspenger.saksbehandling.meldekort.service.GenererMeldeperioderService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.VeilarboppfolgingGateway
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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.TiltaksdeltakerService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.arena.TiltaksdeltakerArenaConsumer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.jobb.EndretTiltaksdeltakerJobb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.komet.TiltaksdeltakerKometConsumer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository.TiltaksdeltakerKafkaRepository
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup.UtbetalingContext
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

    @Suppress("UNCHECKED_CAST")
    open val tokenService: TokenService by lazy {
        val tokenVerificationToken = Configuration.TokenVerificationConfig()
        MicrosoftEntraIdTokenService(
            url = tokenVerificationToken.jwksUri,
            issuer = tokenVerificationToken.issuer,
            clientId = tokenVerificationToken.clientId,
            autoriserteBrukerroller = tokenVerificationToken.roles,
            systembrukerMapper = ::systembrukerMapper as (String, String, Set<String>) -> GenerellSystembruker<
                GenerellSystembrukerrolle,
                GenerellSystembrukerroller<GenerellSystembrukerrolle>,
                >,
            inkluderScopes = false,
        )
    }
    open val entraIdSystemtokenClient: EntraIdSystemtokenClient by lazy {
        EntraIdSystemtokenHttpClient(
            baseUrl = Configuration.azureOpenidConfigTokenEndpoint,
            clientId = Configuration.clientId,
            clientSecret = Configuration.clientSecret,
        )
    }
    open val veilarboppfolgingGateway: VeilarboppfolgingGateway by lazy {
        VeilarboppfolgingHttpClient(
            baseUrl = Configuration.veilarboppfolgingUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.veilarboppfolgingScope) },
        )
    }
    open val navkontorService: NavkontorService by lazy { NavkontorService(veilarboppfolgingGateway) }
    open val oppgaveGateway: OppgaveGateway by lazy {
        OppgaveHttpClient(
            baseUrl = Configuration.oppgaveUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.oppgaveScope) },
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
            oppgaveGateway = oppgaveGateway,
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
            personGateway = personContext.personGateway,
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
            oppgaveGateway = oppgaveGateway,
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
        )
    }

    open val personContext by lazy { PersonContext(sessionFactory, entraIdSystemtokenClient) }
    open val dokumentContext by lazy { DokumentContext(entraIdSystemtokenClient) }
    open val statistikkContext by lazy { StatistikkContext(sessionFactory, personContext.tilgangsstyringService, gitHash, clock) }
    open val søknadContext by lazy { SøknadContext(sessionFactory, oppgaveGateway) }
    open val tiltakContext by lazy { TiltaksdeltagelseContext(entraIdSystemtokenClient) }
    open val profile by lazy { Configuration.applicationProfile() }
    open val sakContext by lazy {
        SakContext(
            sessionFactory = sessionFactory,
            tilgangsstyringService = personContext.tilgangsstyringService,
            poaoTilgangGateway = personContext.poaoTilgangGateway,
            personService = personContext.personService,
            profile = profile,
            clock = clock,
        )
    }
    open val utbetalingContext by lazy {
        UtbetalingContext(
            sessionFactory = sessionFactory,
            genererUtbetalingsvedtakGateway = dokumentContext.genererUtbetalingsvedtakGateway,
            journalførMeldekortGateway = dokumentContext.journalførMeldekortGateway,
            entraIdSystemtokenClient = entraIdSystemtokenClient,
            navIdentClient = personContext.navIdentClient,
            sakRepo = sakContext.sakRepo,
            clock = clock,
        )
    }
    open val meldekortContext by lazy {
        MeldekortContext(
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
        )
    }
    open val behandlingContext by lazy {
        BehandlingOgVedtakContext(
            sessionFactory = sessionFactory,
            meldekortBehandlingRepo = meldekortContext.meldekortBehandlingRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            journalførVedtaksbrevGateway = dokumentContext.journalførVedtaksbrevGateway,
            genererVedtaksbrevGateway = dokumentContext.genererInnvilgelsesvedtaksbrevGateway,
            genererStansvedtaksbrevGateway = dokumentContext.genererStansvedtaksbrevGateway,
            tilgangsstyringService = personContext.tilgangsstyringService,
            dokumentdistribusjonsklient = dokumentContext.dokumentdistribusjonsklient,
            personService = personContext.personService,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltaksdeltagelseGateway = tiltakContext.tiltaksdeltagelseGateway,
            oppgaveGateway = oppgaveGateway,
            clock = clock,
            statistikkSakService = statistikkContext.statistikkSakService,
        )
    }

    private val datadelingGateway by lazy {
        DatadelingHttpClient(
            baseUrl = Configuration.datadelingUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.datadelingScope) },
        )
    }

    val sendTilDatadelingService by lazy {
        SendTilDatadelingService(
            rammevedtakRepo = behandlingContext.rammevedtakRepo,
            behandlingRepo = behandlingContext.behandlingRepo,
            datadelingClient = datadelingGateway,
            clock = clock,
        )
    }

    val mottaBrukerutfyltMeldekortService by lazy {
        MottaBrukerutfyltMeldekortService(
            brukersMeldekortRepo = meldekortContext.brukersMeldekortRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            kanBehandleAutomatisk = !Configuration.isProd(),
        )
    }

    val avbrytSøknadOgBehandlingContext by lazy {
        AvbrytSøknadOgBehandlingContext(
            sakService = sakContext.sakService,
            søknadService = søknadContext.søknadService,
            behandlingService = behandlingContext.behandlingService,
            sessionFactory = sessionFactory,
        )
    }

    val genererMeldeperioderService by lazy {
        GenererMeldeperioderService(
            sakRepo = sakContext.sakRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }
}
