package no.nav.tiltakspenger.saksbehandling.context

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenHttpClient
import no.nav.tiltakspenger.libs.auth.core.MicrosoftEntraIdTokenService
import no.nav.tiltakspenger.libs.auth.core.TokenService
import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.saksbehandling.Configuration
import no.nav.tiltakspenger.saksbehandling.auth.systembrukerMapper
import no.nav.tiltakspenger.saksbehandling.clients.oppgave.OppgaveHttpClient
import no.nav.tiltakspenger.saksbehandling.clients.veilarboppfolging.VeilarboppfolgingHttpClient
import no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingHttpClient
import no.nav.tiltakspenger.saksbehandling.db.DataSourceSetup
import no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.TiltaksdeltakerService
import no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.arena.TiltaksdeltakerArenaConsumer
import no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.jobb.EndretTiltaksdeltakerJobb
import no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.komet.TiltaksdeltakerKometConsumer
import no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaRepository
import no.nav.tiltakspenger.saksbehandling.meldekort.service.GenererMeldeperioderService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.MottaBrukerutfyltMeldekortService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.VeilarboppfolgingGateway
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.NavkontorService

/**
 * Inneholder alle klienter, repoer og servicer.
 * Tanken er at man kan erstatte klienter og repoer med Fakes for testing.
 */
@Suppress("unused")
open class ApplicationContext(
    internal val gitHash: String,
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

    open val personContext by lazy { PersonContext(sessionFactory, entraIdSystemtokenClient) }
    open val dokumentContext by lazy { DokumentContext(entraIdSystemtokenClient) }
    open val statistikkContext by lazy { StatistikkContext(sessionFactory) }
    open val søknadContext by lazy { SøknadContext(sessionFactory, oppgaveGateway) }
    open val tiltakContext by lazy { TiltakContext(entraIdSystemtokenClient) }
    open val profile by lazy { Configuration.applicationProfile() }
    open val sakContext by lazy {
        SakContext(
            sessionFactory = sessionFactory,
            tilgangsstyringService = personContext.tilgangsstyringService,
            poaoTilgangGateway = personContext.poaoTilgangGateway,
            personService = personContext.personService,
            profile = profile,
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
        )
    }
    open val behandlingContext by lazy {
        FørstegangsbehandlingContext(
            sessionFactory = sessionFactory,
            meldekortBehandlingRepo = meldekortContext.meldekortBehandlingRepo,
            meldeperiodeRepo = meldekortContext.meldeperiodeRepo,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            statistikkStønadRepo = statistikkContext.statistikkStønadRepo,
            gitHash = gitHash,
            journalførVedtaksbrevGateway = dokumentContext.journalførVedtaksbrevGateway,
            genererVedtaksbrevGateway = dokumentContext.genererInnvilgelsesvedtaksbrevGateway,
            genererStansvedtaksbrevGateway = dokumentContext.genererStansvedtaksbrevGateway,
            tilgangsstyringService = personContext.tilgangsstyringService,
            dokdistGateway = dokumentContext.dokdistGateway,
            personService = personContext.personService,
            navIdentClient = personContext.navIdentClient,
            sakService = sakContext.sakService,
            tiltakGateway = tiltakContext.tiltakGateway,
            oppgaveGateway = oppgaveGateway,
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
        )
    }

    val mottaBrukerutfyltMeldekortService by lazy {
        MottaBrukerutfyltMeldekortService(
            brukersMeldekortRepo = meldekortContext.brukersMeldekortRepo,
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
        )
    }
}
