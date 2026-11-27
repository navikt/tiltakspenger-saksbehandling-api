package no.nav.tiltakspenger.saksbehandling.infra.setup

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.TilbakekrevingshendelsePolicy
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import java.time.LocalDate
import java.time.LocalDateTime

enum class EnvironmentProfile {
    LOCAL,

    /** En Nais-klynge ut over prod, med konfigurasjon fra en [Miljøkonfigurasjonsleverandør]. */
    ANNEN_NAIS_KLYNGE,
    PROD,
}

private const val WIREMOCK_URL = "http://host.docker.internal:8091"

data class HelvedSimuleringKonfigurasjon(
    val url: String,
    val scope: String,
) {
    companion object {
        val Lokal = HelvedSimuleringKonfigurasjon(WIREMOCK_URL, "localhost")
        val Prod = HelvedSimuleringKonfigurasjon("http://simulering.helved", "prod-gcp:helved:simulering")
    }
}

interface EnvironmentConfig {
    val environmentProfile: EnvironmentProfile
    val httpPort: Int
    val logbackConfigurationFile: String

    val roleSaksbehandler: String
    val roleBeslutter: String
    val roleVeileder: String
    val roleUtvikler: String
    val roleTilbakekreving: String

    val electorPath: String
    val appImage: String
    val dbJdbcUrl: String

    val tokenEndpoint: String
    val tokenIntrospectionEndpoint: String
    val tokenExchangeEndpoint: String

    val pdlScope: String
    val pdlUrl: String

    val skjermingScope: String
    val skjermingUrl: String

    val tiltakshistorikkScope: String
    val tiltakshistorikkUrl: String

    val utbetalingScope: String
    val utbetalingUrl: String

    val helvedSimuleringScope: String
    val helvedSimuleringUrl: String

    val dokarkivScope: String
    val dokarkivUrl: String

    val dokdistScope: String
    val dokdistUrl: String

    val pdfgenrsUrl: String

    val microsoftScope: String
    val microsoftUrl: String

    val datadelingScope: String
    val datadelingUrl: String

    val meldekortApiScope: String
    val meldekortApiUrl: String

    val soknadApiScope: String
    val soknadApiUrl: String

    val aoKontorScope: String
    val aoKontorUrl: String

    val oppgaveScope: String
    val oppgaveUrl: String

    val sokosUtbetaldataScope: String
    val sokosUtbetaldataUrl: String

    val tilgangsmaskinenScope: String
    val tilgangsmaskinenUrl: String

    val entraProxyScope: String
    val entraProxyUrl: String

    val tiltakspengerArenaScope: String
    val tiltakspengerArenaUrl: String

    val safScope: String
    val safUrl: String

    val kabalScope: String
    val kabalUrl: String

    val arenaTiltaksdeltakerTopic: String
    val kometTiltaksdeltakerTopic: String
    val teamTiltakTiltaksdeltakerTopic: String
    val leesahTopic: String
    val aktorV2Topic: String
    val identhendelseTopic: String
    val tilbakekrevingTopic: String
    val utbetalingTopic: String
    val helvedStatusTopic: String

    /** Hvilken versjon av Hel Veds API nye saker sender utbetalinger med. */
    val oppdragsversjonForNyeSaker: Oppdragsversjon

    val saksnummerGenerator: SaksnummerGenerator

    /** Utbetalingsoversikten følger bare opp saker opprettet fra og med dette tidspunktet, og null betyr alle saker. */
    val utbetalingerFølgesOppFra: LocalDateTime?

    val tilbakekrevingshendelsePolicy: TilbakekrevingshendelsePolicy

    val leesahAvroSerializablePackage: String
    val aktorV2AvroSerializablePackage: String

    val saksbehandlingFrontendUrl: String

    /** Google Cloud-prosjektet appens logger ligger i, og null når appen ikke logger til Google Cloud. */
    val gcpProsjektId: String?
}

private data object LokalSaksnummerGenerator : SaksnummerGenerator {
    override fun generer(dato: LocalDate): Saksnummer = Saksnummer.genererSaknummer(dato = dato, løpenr = "1001")
}

data object LocalConfig : EnvironmentConfig {
    /**
     *  Benyttes kun dersom appen kjører lokalt med "prod-main" i App.kt.
     *  Ved kjøring via LokalMain.kt vil normalt fake-klienter benyttes istedenfor å kalle disse url'ene
     *
     *  Wiremock kan kjøres opp via docker-compose i meta-repoet
     * */
    override val environmentProfile = EnvironmentProfile.LOCAL
    override val httpPort = 8080
    override val logbackConfigurationFile = "logback.local.xml"

    override val roleSaksbehandler = "1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"
    override val roleBeslutter = "79985315-b2de-40b8-a740-9510796993c6"
    override val roleVeileder = "13d39d54-4af9-44a8-a57c-e223df62ab86"
    override val roleUtvikler = "8e07be64-c44a-4730-8053-3910ff4e3e92"
    override val roleTilbakekreving = "bdfbab87-3016-4341-97ca-8053ff1e3962"

    // Brukes ikke lokalt
    override val electorPath = ""

    // Image-referanse på formen repo/navn:tag — kun git-hashen etter siste kolon brukes (Configuration.gitHash).
    override val appImage = "lokalt-bygg:githubhash"
    override val dbJdbcUrl = "jdbc:postgresql://host.docker.internal:5433/saksbehandling?user=postgres&password=test"

    override val tokenEndpoint = "http://localhost:7165/api/v1/token"
    override val tokenIntrospectionEndpoint = "http://localhost:7165/api/v1/introspect"
    override val tokenExchangeEndpoint = "http://localhost:7165/api/v1/token/exchange"

    override val pdlScope = "localhost"
    override val pdlUrl = "$WIREMOCK_URL/graphql"

    override val skjermingScope = "localhost"
    override val skjermingUrl = WIREMOCK_URL

    override val tiltakshistorikkScope = "localhost"
    override val tiltakshistorikkUrl = WIREMOCK_URL

    override val utbetalingScope = "localhost"
    override val utbetalingUrl = WIREMOCK_URL

    override val helvedSimuleringScope = HelvedSimuleringKonfigurasjon.Lokal.scope
    override val helvedSimuleringUrl = HelvedSimuleringKonfigurasjon.Lokal.url

    override val dokarkivScope = "localhost"
    override val dokarkivUrl = WIREMOCK_URL

    override val dokdistScope = "localhost"
    override val dokdistUrl = WIREMOCK_URL

    override val pdfgenrsUrl = "http://host.docker.internal:8084"

    override val microsoftScope = "localhost"
    override val microsoftUrl = WIREMOCK_URL.replaceFirst("http://", "")

    override val datadelingScope = "localhost"
    override val datadelingUrl = "http://host.docker.internal:8082"

    override val meldekortApiScope = "tiltakspenger-meldekort-api"
    override val meldekortApiUrl = "http://localhost:8083"

    override val soknadApiScope = "localhost:tpts:tiltakspenger-soknad-api"
    override val soknadApiUrl = "http://localhost:8085"

    override val aoKontorScope = "localhost"
    override val aoKontorUrl = WIREMOCK_URL

    override val oppgaveScope = "localhost"
    override val oppgaveUrl = WIREMOCK_URL

    override val sokosUtbetaldataScope = "localhost"
    override val sokosUtbetaldataUrl = WIREMOCK_URL

    override val tilgangsmaskinenUrl = WIREMOCK_URL
    override val tilgangsmaskinenScope = "localhost"

    override val entraProxyUrl = WIREMOCK_URL
    override val entraProxyScope = "localhost"

    override val tiltakspengerArenaUrl = WIREMOCK_URL
    override val tiltakspengerArenaScope = "localhost"

    override val safUrl = WIREMOCK_URL
    override val safScope = "localhost"

    override val kabalScope = "localhost"
    override val kabalUrl = WIREMOCK_URL

    override val arenaTiltaksdeltakerTopic = "arena.tiltaksdeltaker"
    override val kometTiltaksdeltakerTopic = "komet.tiltaksdeltaker"
    override val teamTiltakTiltaksdeltakerTopic = "teamtiltak.tiltaksdeltaker"
    override val leesahTopic = "pdl.leesah"
    override val aktorV2Topic = "pdl.aktor"
    override val identhendelseTopic = "tpts.identhendelse"
    override val tilbakekrevingTopic = "tilbake.privat-tilbakekreving-tiltakspenger"
    override val utbetalingTopic = "tpts.utbetaling-v1"
    override val helvedStatusTopic = "helved.status.v1"
    override val oppdragsversjonForNyeSaker = Oppdragsversjon.V2
    override val saksnummerGenerator: SaksnummerGenerator = LokalSaksnummerGenerator
    override val utbetalingerFølgesOppFra: LocalDateTime? = null
    override val tilbakekrevingshendelsePolicy = TilbakekrevingshendelsePolicy.BeholdAlle

    override val leesahAvroSerializablePackage = "no.nav.person.pdl.leesah"
    override val aktorV2AvroSerializablePackage = "no.nav.person.pdl.aktor.v2"

    override val saksbehandlingFrontendUrl = "http://localhost:3000"
    override val gcpProsjektId: String? = null
}

data object ProdConfig : EnvironmentConfig {
    override val environmentProfile = EnvironmentProfile.PROD
    override val httpPort = 8080
    override val logbackConfigurationFile = "logback.xml"

    override val roleSaksbehandler: String = System.getenv("ROLE_SAKSBEHANDLER")
    override val roleBeslutter: String = System.getenv("ROLE_BESLUTTER")
    override val roleVeileder: String = System.getenv("ROLE_VEILEDER")
    override val roleUtvikler: String = System.getenv("ROLE_UTVIKLER")
    override val roleTilbakekreving: String = System.getenv("ROLE_TILBAKEKREVING")

    override val electorPath: String = System.getenv("ELECTOR_PATH")
    override val appImage: String = System.getenv("NAIS_APP_IMAGE")
    override val dbJdbcUrl: String = System.getenv("DB_JDBC_URL")

    override val tokenEndpoint: String = System.getenv("NAIS_TOKEN_ENDPOINT")
    override val tokenIntrospectionEndpoint: String = System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT")
    override val tokenExchangeEndpoint: String = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT")

    override val microsoftScope = "https://graph.microsoft.com/.default"
    override val microsoftUrl = "graph.microsoft.com/v1.0"

    override val pdlScope = "prod-fss:pdl:pdl-api"
    override val pdlUrl = "https://pdl-api.prod-fss-pub.nais.io/graphql"

    override val skjermingScope = "prod-gcp:nom:skjermede-personer-pip"
    override val skjermingUrl = "https://skjermede-personer-pip.intern.nav.no"

    override val tiltakshistorikkScope = "prod-gcp:team-mulighetsrommet:tiltakshistorikk"
    override val tiltakshistorikkUrl = "http://tiltakshistorikk.team-mulighetsrommet"

    override val utbetalingScope = "prod-gcp:helved:utsjekk"
    override val utbetalingUrl = "http://utsjekk.helved"

    override val helvedSimuleringScope = HelvedSimuleringKonfigurasjon.Prod.scope
    override val helvedSimuleringUrl = HelvedSimuleringKonfigurasjon.Prod.url

    override val dokarkivScope = "prod-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv.prod-fss-pub.nais.io"

    override val dokdistScope = "prod-fss:teamdokumenthandtering:dokdistfordeling"
    override val dokdistUrl = "https://dokdistfordeling.prod-fss-pub.nais.io"

    override val pdfgenrsUrl = "http://tiltakspenger-pdfgenrs"

    override val datadelingScope = "prod-gcp:tpts:tiltakspenger-datadeling"
    override val datadelingUrl = "http://tiltakspenger-datadeling"

    override val meldekortApiScope = "prod-gcp:tpts:tiltakspenger-meldekort-api"
    override val meldekortApiUrl = "http://tiltakspenger-meldekort-api"

    override val soknadApiScope = "prod-gcp:tpts:tiltakspenger-soknad-api"
    override val soknadApiUrl = "http://tiltakspenger-soknad-api"

    override val aoKontorScope = "prod-gcp.dab.ao-oppfolgingskontor"
    override val aoKontorUrl = "http://ao-oppfolgingskontor.dab"

    override val oppgaveScope = "prod-fss:oppgavehandtering:oppgave"
    override val oppgaveUrl = "https://oppgave.prod-fss-pub.nais.io"

    override val sokosUtbetaldataScope = "prod-fss:okonomi:sokos-utbetaldata"
    override val sokosUtbetaldataUrl = "https://sokos-utbetaldata.prod-fss-pub.nais.io"

    override val tilgangsmaskinenUrl = "http://populasjonstilgangskontroll.tilgangsmaskin"
    override val tilgangsmaskinenScope = "api://prod-gcp.tilgangsmaskin.populasjonstilgangskontroll/.default"

    override val entraProxyUrl = "http://entra-proxy.sikkerhetstjenesten"
    override val entraProxyScope = "api://prod-gcp.sikkerhetstjenesten.entra-proxy/.default"

    override val tiltakspengerArenaUrl = "https://tiltakspenger-arena.prod-fss-pub.nais.io"
    override val tiltakspengerArenaScope = "prod-fss:tpts:tiltakspenger-arena"

    override val safUrl = "https://saf.prod-fss-pub.nais.io"
    override val safScope = "api://prod-fss.teamdokumenthandtering.saf/.default"

    override val kabalScope = "api://prod-gcp.klage.kabal-api/.default"
    override val kabalUrl = "https://kabal-api.intern.nav.no"

    override val arenaTiltaksdeltakerTopic = "teamarenanais.aapen-arena-tiltakdeltakerendret-v1-p"
    override val kometTiltaksdeltakerTopic = "amt.deltaker-v1"
    override val teamTiltakTiltaksdeltakerTopic = "arbeidsgiver.tiltak-avtale-hendelse-compact"
    override val leesahTopic = "pdl.leesah-v1"
    override val aktorV2Topic = "pdl.aktor-v2"
    override val identhendelseTopic = "tpts.identhendelse-v1"
    override val tilbakekrevingTopic = "tilbake.privat-tilbakekreving-tiltakspenger"
    override val utbetalingTopic = "tpts.utbetaling-v1"
    override val helvedStatusTopic = "helved.status.v1"
    override val oppdragsversjonForNyeSaker = Oppdragsversjon.V2
    override val saksnummerGenerator = SaksnummerGenerator.Prod
    override val utbetalingerFølgesOppFra: LocalDateTime? = null
    override val tilbakekrevingshendelsePolicy = TilbakekrevingshendelsePolicy.BeholdAlle

    override val leesahAvroSerializablePackage = "no.nav.person.pdl.leesah"
    override val aktorV2AvroSerializablePackage = "no.nav.person.pdl.aktor.v2"

    override val saksbehandlingFrontendUrl = "https://tiltakspenger-saksbehandling.ansatt.nav.no"
    override val gcpProsjektId = "tpts-prod-b5ff"
}
