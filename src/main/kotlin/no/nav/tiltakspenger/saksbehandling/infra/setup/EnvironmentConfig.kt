package no.nav.tiltakspenger.saksbehandling.infra.setup

enum class EnvironmentProfile {
    LOCAL,
    DEV,
    PROD,
}

sealed interface EnvironmentConfig {
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

    val tiltakScope: String
    val tiltakUrl: String

    val tiltakshistorikkScope: String
    val tiltakshistorikkUrl: String

    val utbetalingScope: String
    val utbetalingUrl: String

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

    val aoKontorScope: String
    val aoKontorUrl: String

    val veilarboppfolgingScope: String
    val veilarboppfolgingUrl: String

    val oppgaveScope: String
    val oppgaveUrl: String

    val sokosUtbetaldataScope: String
    val sokosUtbetaldataUrl: String

    val tilgangsmaskinenScope: String
    val tilgangsmaskinenUrl: String

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

    val leesahAvroSerializablePackage: String
    val aktorV2AvroSerializablePackage: String

    val saksbehandlingFrontendUrl: String
}

data object LocalConfig : EnvironmentConfig {
    /**
     *  Benyttes kun dersom appen kjører lokalt med "prod-main" i App.kt.
     *  Ved kjøring via LokalMain.kt vil normalt fake-klienter benyttes istedenfor å kalle disse url'ene
     *
     *  Wiremock kan kjøres opp via docker-compose i meta-repoet
     * */
    private const val WIREMOCK_URL = "http://host.docker.internal:8091"

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

    override val tiltakScope = "localhost"
    override val tiltakUrl = WIREMOCK_URL

    override val tiltakshistorikkScope = "localhost"
    override val tiltakshistorikkUrl = WIREMOCK_URL

    override val utbetalingScope = "localhost"
    override val utbetalingUrl = WIREMOCK_URL

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

    override val aoKontorScope = "localhost"
    override val aoKontorUrl = WIREMOCK_URL

    override val veilarboppfolgingScope = "localhost"
    override val veilarboppfolgingUrl = WIREMOCK_URL

    override val oppgaveScope = "localhost"
    override val oppgaveUrl = WIREMOCK_URL

    override val sokosUtbetaldataScope = "localhost"
    override val sokosUtbetaldataUrl = WIREMOCK_URL

    override val tilgangsmaskinenUrl = WIREMOCK_URL
    override val tilgangsmaskinenScope = "localhost"

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

    override val leesahAvroSerializablePackage = "no.nav.person.pdl.leesah"
    override val aktorV2AvroSerializablePackage = "no.nav.person.pdl.aktor.v2"

    override val saksbehandlingFrontendUrl = "http://localhost:3000"
}

data object DevConfig : EnvironmentConfig {
    override val environmentProfile = EnvironmentProfile.DEV
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

    override val pdlScope = "dev-fss:pdl:pdl-api"
    override val pdlUrl = "https://pdl-api.dev-fss-pub.nais.io/graphql"

    override val skjermingScope = "dev-gcp:nom:skjermede-personer-pip"
    override val skjermingUrl = "https://skjermede-personer-pip.intern.dev.nav.no"

    override val tiltakScope = "dev-gcp:tpts:tiltakspenger-tiltak"
    override val tiltakUrl = "http://tiltakspenger-tiltak"

    override val tiltakshistorikkScope = "dev-gcp:team-mulighetsrommet:tiltakshistorikk"
    override val tiltakshistorikkUrl = "http://tiltakshistorikk.team-mulighetsrommet"

    override val utbetalingScope = "dev-gcp:helved:utsjekk"
    override val utbetalingUrl = "http://utsjekk.helved"

    override val dokarkivScope = "dev-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv-q2.dev-fss-pub.nais.io"

    override val dokdistScope = "dev-fss:teamdokumenthandtering:dokdistfordeling"
    override val dokdistUrl = "https://dokdistfordeling.dev-fss-pub.nais.io"

    override val pdfgenrsUrl = "http://tiltakspenger-pdfgenrs"

    override val datadelingScope = "dev-gcp:tpts:tiltakspenger-datadeling"
    override val datadelingUrl = "http://tiltakspenger-datadeling"

    override val meldekortApiScope = "dev-gcp:tpts:tiltakspenger-meldekort-api"
    override val meldekortApiUrl = "http://tiltakspenger-meldekort-api"

    override val aoKontorScope = "dev-gcp.dab.ao-oppfolgingskontor"
    override val aoKontorUrl = "http://ao-oppfolgingskontor.dab"

    override val veilarboppfolgingScope = "dev-gcp:poao:veilarboppfolging"
    override val veilarboppfolgingUrl = "http://veilarboppfolging.poao"

    override val oppgaveScope = "dev-fss:oppgavehandtering:oppgave"
    override val oppgaveUrl = "https://oppgave.dev-fss-pub.nais.io"

    override val sokosUtbetaldataScope = "dev-fss:okonomi:sokos-utbetaldata"
    override val sokosUtbetaldataUrl = "https://sokos-utbetaldata.dev-fss-pub.nais.io"

    override val tilgangsmaskinenUrl = "http://populasjonstilgangskontroll.tilgangsmaskin"
    override val tilgangsmaskinenScope = "api://dev-gcp.tilgangsmaskin.populasjonstilgangskontroll/.default"

    override val tiltakspengerArenaUrl = "https://tiltakspenger-arena.dev-fss-pub.nais.io"
    override val tiltakspengerArenaScope = "dev-fss:tpts:tiltakspenger-arena"

    override val safUrl = "https://saf-q2.dev-fss-pub.nais.io"
    override val safScope = "api://dev-fss.teamdokumenthandtering.saf/.default"

    override val kabalScope = "api://dev-gcp.klage.kabal-api/.default"
    override val kabalUrl = "https://kabal-api.intern.dev.nav.no"

    override val arenaTiltaksdeltakerTopic = "teamarenanais.aapen-arena-tiltakdeltakerendret-v1-q2"
    override val kometTiltaksdeltakerTopic = "amt.deltaker-v1"
    override val teamTiltakTiltaksdeltakerTopic = "arbeidsgiver.tiltak-avtale-hendelse-compact"
    override val leesahTopic = "pdl.leesah-v1"
    override val aktorV2Topic = "pdl.aktor-v2"
    override val identhendelseTopic = "tpts.identhendelse-v1"
    override val tilbakekrevingTopic = "tilbake.privat-tilbakekreving-tiltakspenger"

    override val leesahAvroSerializablePackage = "no.nav.person.pdl.leesah"
    override val aktorV2AvroSerializablePackage = "no.nav.person.pdl.aktor.v2"

    override val saksbehandlingFrontendUrl = "https://tiltakspenger-saksbehandling.ansatt.dev.nav.no"
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

    override val tiltakScope = "prod-gcp:tpts:tiltakspenger-tiltak"
    override val tiltakUrl = "http://tiltakspenger-tiltak"

    override val tiltakshistorikkScope = "prod-gcp:team-mulighetsrommet:tiltakshistorikk"
    override val tiltakshistorikkUrl = "http://tiltakshistorikk.team-mulighetsrommet"

    override val utbetalingScope = "prod-gcp:helved:utsjekk"
    override val utbetalingUrl = "http://utsjekk.helved"

    override val dokarkivScope = "prod-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv.prod-fss-pub.nais.io"

    override val dokdistScope = "prod-fss:teamdokumenthandtering:dokdistfordeling"
    override val dokdistUrl = "https://dokdistfordeling.prod-fss-pub.nais.io"

    override val pdfgenrsUrl = "http://tiltakspenger-pdfgenrs"

    override val datadelingScope = "prod-gcp:tpts:tiltakspenger-datadeling"
    override val datadelingUrl = "http://tiltakspenger-datadeling"

    override val meldekortApiScope = "prod-gcp:tpts:tiltakspenger-meldekort-api"
    override val meldekortApiUrl = "http://tiltakspenger-meldekort-api"

    override val aoKontorScope = "prod-gcp.dab.ao-oppfolgingskontor"
    override val aoKontorUrl = "http://ao-oppfolgingskontor.dab"

    override val veilarboppfolgingScope = "prod-gcp:poao:veilarboppfolging"
    override val veilarboppfolgingUrl = "http://veilarboppfolging.poao"

    override val oppgaveScope = "prod-fss:oppgavehandtering:oppgave"
    override val oppgaveUrl = "https://oppgave.prod-fss-pub.nais.io"

    override val sokosUtbetaldataScope = "prod-fss:okonomi:sokos-utbetaldata"
    override val sokosUtbetaldataUrl = "https://sokos-utbetaldata.prod-fss-pub.nais.io"

    override val tilgangsmaskinenUrl = "http://populasjonstilgangskontroll.tilgangsmaskin"
    override val tilgangsmaskinenScope = "api://prod-gcp.tilgangsmaskin.populasjonstilgangskontroll/.default"

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

    override val leesahAvroSerializablePackage = "no.nav.person.pdl.leesah"
    override val aktorV2AvroSerializablePackage = "no.nav.person.pdl.aktor.v2"

    override val saksbehandlingFrontendUrl = "https://tiltakspenger-saksbehandling.ansatt.nav.no"
}
