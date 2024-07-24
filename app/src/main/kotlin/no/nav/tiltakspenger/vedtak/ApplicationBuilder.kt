package no.nav.tiltakspenger.vedtak

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.kvp.KvpVilkårServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.personopplysning.PersonopplysningServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.sak.SakServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.søker.SøkerServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.utbetaling.UtbetalingServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.vedtak.VedtakServiceImpl
import no.nav.tiltakspenger.vedtak.auth.AzureTokenProvider
import no.nav.tiltakspenger.vedtak.clients.brevpublisher.BrevPublisherGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.meldekort.MeldekortGrunnlagGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.person.PersonHttpklient
import no.nav.tiltakspenger.vedtak.clients.skjerming.SkjermingClientImpl
import no.nav.tiltakspenger.vedtak.clients.skjerming.SkjermingGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.tiltak.TiltakClientImpl
import no.nav.tiltakspenger.vedtak.clients.tiltak.TiltakGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.utbetaling.UtbetalingClient
import no.nav.tiltakspenger.vedtak.clients.utbetaling.UtbetalingGatewayImpl
import no.nav.tiltakspenger.vedtak.db.DataSource
import no.nav.tiltakspenger.vedtak.db.flywayMigrate
import no.nav.tiltakspenger.vedtak.repository.attestering.AttesteringRepoImpl
import no.nav.tiltakspenger.vedtak.repository.behandling.KravdatoSaksopplysningRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.PostgresBehandlingRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.SaksopplysningRepo
import no.nav.tiltakspenger.vedtak.repository.behandling.TiltakDAO
import no.nav.tiltakspenger.vedtak.repository.behandling.UtfallsperiodeDAO
import no.nav.tiltakspenger.vedtak.repository.behandling.VurderingRepo
import no.nav.tiltakspenger.vedtak.repository.sak.PersonopplysningerBarnMedIdentRepo
import no.nav.tiltakspenger.vedtak.repository.sak.PersonopplysningerBarnUtenIdentRepo
import no.nav.tiltakspenger.vedtak.repository.sak.PostgresPersonopplysningerRepo
import no.nav.tiltakspenger.vedtak.repository.sak.PostgresSakRepo
import no.nav.tiltakspenger.vedtak.repository.søker.PersonopplysningerDAO
import no.nav.tiltakspenger.vedtak.repository.søker.SøkerRepositoryImpl
import no.nav.tiltakspenger.vedtak.repository.søknad.BarnetilleggDAO
import no.nav.tiltakspenger.vedtak.repository.søknad.SøknadDAO
import no.nav.tiltakspenger.vedtak.repository.søknad.SøknadTiltakDAO
import no.nav.tiltakspenger.vedtak.repository.søknad.VedleggDAO
import no.nav.tiltakspenger.vedtak.repository.vedtak.VedtakRepoImpl
import no.nav.tiltakspenger.vedtak.routes.vedtakApi
import no.nav.tiltakspenger.vedtak.tilgang.JWTInnloggetSaksbehandlerProvider
import no.nav.tiltakspenger.vedtak.tilgang.JWTInnloggetSystembrukerProvider

val log = KotlinLogging.logger {}
val securelog = KotlinLogging.logger("tjenestekall")

internal class ApplicationBuilder(@Suppress("UNUSED_PARAMETER") config: Map<String, String>) :
    RapidsConnection.StatusListener {
    private val rapidConfig = if (Configuration.applicationProfile() == Profile.LOCAL) {
        RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidsAndRivers, LokalKafkaConfig())
    } else {
        RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidsAndRivers)
    }
    val rapidsConnection: RapidsConnection = RapidApplication.Builder(rapidConfig)
        .withKtorModule {
            vedtakApi(
                config = Configuration.TokenVerificationConfig(),
                innloggetSaksbehandlerProvider = JWTInnloggetSaksbehandlerProvider(),
                innloggetSystembrukerProvider = JWTInnloggetSystembrukerProvider(),
                søkerService = søkerService,
                sakService = sakService,
                behandlingService = behandlingService,
                attesteringRepo = attesteringRepo,
                kvpVilkårService = kvpVilkårService,
                livsoppholdVilkårService = livsoppholdVilkårService,
            )
        }
        .build()

    private val tokenProviderUtbetaling: AzureTokenProvider =
        AzureTokenProvider(config = Configuration.oauthConfigUtbetaling())
    private val tokenProviderPdl: AzureTokenProvider =
        AzureTokenProvider(config = Configuration.ouathConfigPdl())
    private val tokenProviderSkjerming: AzureTokenProvider =
        AzureTokenProvider(config = Configuration.oauthConfigSkjerming())
    private val tokenProviderTiltak: AzureTokenProvider =
        AzureTokenProvider(config = Configuration.oauthConfigTiltak())

    private val dataSource = DataSource.hikariDataSource
    private val sessionCounter = SessionCounter(log)
    private val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)

    private val utbetalingClient = UtbetalingClient(getToken = tokenProviderUtbetaling::getToken)
    private val skjermingClient = SkjermingClientImpl(getToken = tokenProviderSkjerming::getToken)
    private val tiltakClient = TiltakClientImpl(getToken = tokenProviderTiltak::getToken)
    private val skjermingGateway = SkjermingGatewayImpl(skjermingClient)
    private val utbetalingGateway = UtbetalingGatewayImpl(utbetalingClient)
    private val tiltakGateway = TiltakGatewayImpl(tiltakClient)
    private val brevPublisherGateway = BrevPublisherGatewayImpl(rapidsConnection)
    private val meldekortGrunnlagGateway = MeldekortGrunnlagGatewayImpl(rapidsConnection)
    private val personGateway =
        PersonHttpklient(endepunkt = Configuration.pdlClientConfig().baseUrl, azureTokenProvider = tokenProviderPdl)

    private val personopplysningerDAO = PersonopplysningerDAO()
    private val søkerRepository = SøkerRepositoryImpl(sessionFactory, personopplysningerDAO)
    private val barnMedIdentDAO = PersonopplysningerBarnMedIdentRepo()
    private val barnUtenIdentDAO = PersonopplysningerBarnUtenIdentRepo()
    private val personopplysningRepo = PostgresPersonopplysningerRepo(sessionFactory, barnMedIdentDAO, barnUtenIdentDAO)
    private val saksopplysningRepo = SaksopplysningRepo()
    private val vurderingRepo = VurderingRepo()
    private val barnetilleggDAO = BarnetilleggDAO()
    private val søknadTiltakDAO = SøknadTiltakDAO()
    private val vedleggDAO = VedleggDAO()
    private val tiltakDAO = TiltakDAO()
    private val utfallsperiodeDAO = UtfallsperiodeDAO()
    private val kravdatoSaksopplysningRepo = KravdatoSaksopplysningRepo()
    private val søknadDAO = SøknadDAO(
        barnetilleggDAO = barnetilleggDAO,
        tiltakDAO = søknadTiltakDAO,
        vedleggDAO = vedleggDAO,
    )
    private val behandlingRepo = PostgresBehandlingRepo(
        sessionFactory = sessionFactory,
        saksopplysningRepo = saksopplysningRepo,
        vurderingRepo = vurderingRepo,
        søknadDAO = søknadDAO,
        tiltakDAO = tiltakDAO,
        utfallsperiodeDAO = utfallsperiodeDAO,
        kravdatoSaksopplysningRepo = kravdatoSaksopplysningRepo,
    )

    private val vedtakRepo = VedtakRepoImpl(
        behandlingRepo = behandlingRepo,
        utfallsperiodeDAO = utfallsperiodeDAO,
        sessionFactory = sessionFactory,
    )

    private val sakRepo = PostgresSakRepo(
        personopplysningerRepo = personopplysningRepo,
        behandlingRepo = behandlingRepo,
        vedtakRepo = vedtakRepo,
        sessionFactory = sessionFactory,
    )

    private val attesteringRepo = AttesteringRepoImpl(
        sessionFactory = sessionFactory,
    )

    private val utbetalingService = UtbetalingServiceImpl(utbetalingGateway)
    private val vedtakService = VedtakServiceImpl(vedtakRepo)
    private val søkerService = SøkerServiceImpl(søkerRepository)
    private val personopplysningServiceImpl = PersonopplysningServiceImpl(personopplysningRepo)

    private val behandlingService = BehandlingServiceImpl(
        behandlingRepo = behandlingRepo,
        vedtakRepo = vedtakRepo,
        personopplysningRepo = personopplysningRepo,
        utbetalingService = utbetalingService,
        brevPublisherGateway = brevPublisherGateway,
        meldekortGrunnlagGateway = meldekortGrunnlagGateway,
        tiltakGateway = tiltakGateway,
        sakRepo = sakRepo,
        attesteringRepo = attesteringRepo,
        sessionFactory = sessionFactory,
    )
    private val sakService =
        SakServiceImpl(
            sakRepo = sakRepo,
            behandlingRepo = behandlingRepo,
            behandlingService = behandlingService,
            personGateway = personGateway,
            skjermingGateway = skjermingGateway,
            søkerRepository = søkerRepository,
        )
    private val kvpVilkårService = KvpVilkårServiceImpl(
        behandlingService = behandlingService,
        behandlingRepo = behandlingRepo,
    )
    private val livsoppholdVilkårService = LivsoppholdVilkårServiceImpl(
        behandlingService = behandlingService,
        behandlingRepo = behandlingRepo,
    )

    init {
        rapidsConnection.register(this)
    }

    fun start() {
        rapidsConnection.start()
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        log.info("Shutdown")
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        log.info("Skal kjøre flyway migrering")
        flywayMigrate()
        log.info("Har kjørt flyway migrering")
    }
}
