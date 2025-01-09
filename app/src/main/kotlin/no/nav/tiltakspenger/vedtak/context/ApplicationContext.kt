package no.nav.tiltakspenger.vedtak.context

import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.service.SendTilDatadelingService
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
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.auth.systembrukerMapper
import no.nav.tiltakspenger.vedtak.clients.datadeling.DatadelingHttpClient
import no.nav.tiltakspenger.vedtak.db.DataSourceSetup

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
    open val personContext by lazy { PersonContext(sessionFactory, entraIdSystemtokenClient) }
    open val dokumentContext by lazy { DokumentContext(entraIdSystemtokenClient) }
    open val statistikkContext by lazy { StatistikkContext(sessionFactory) }
    open val søknadContext by lazy { SøknadContext(sessionFactory) }
    open val tiltakContext by lazy { TiltakContext(entraIdSystemtokenClient) }
    open val profile by lazy { Configuration.applicationProfile() }
    open val sakContext by lazy {
        SakContext(
            sessionFactory = sessionFactory,
            søknadService = søknadContext.søknadService,
            statistikkSakRepo = statistikkContext.statistikkSakRepo,
            tilgangsstyringService = personContext.tilgangsstyringService,
            poaoTilgangGateway = personContext.poaoTilgangGateway,
            tiltakGateway = tiltakContext.tiltakGateway,
            personService = personContext.personService,
            gitHash = gitHash,
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
        )
    }
    open val behandlingContext by lazy {
        FørstegangsbehandlingContext(
            sessionFactory = sessionFactory,
            meldekortRepo = meldekortContext.meldekortRepo,
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
            datadelingGateway = datadelingGateway,
        )
    }
}
