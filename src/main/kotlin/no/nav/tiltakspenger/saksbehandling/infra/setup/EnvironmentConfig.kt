package no.nav.tiltakspenger.saksbehandling.infra.setup

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

sealed interface EnvironmentConfig {
    val profile: Profile
    val httpPort: Int
    val logbackConfigurationFile: String

    val roleSaksbehandler: String
    val roleBeslutter: String
    val roleDrift: String

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

    val utbetalingScope: String
    val utbetalingUrl: String

    val dokarkivScope: String
    val dokarkivUrl: String

    val dokdistScope: String
    val dokdistUrl: String

    val pdfgenUrl: String
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
}

data object LocalConfig : EnvironmentConfig {
    override val profile = Profile.LOCAL
    override val httpPort = 8080
    override val logbackConfigurationFile = "logback.local.xml"

    override val roleSaksbehandler = "1b3a2c4d-d620-4fcf-a29b-a6cdadf29680"
    override val roleBeslutter = "79985315-b2de-40b8-a740-9510796993c6"
    override val roleDrift = "c511113e-5b22-49e7-b9c4-eeb23b01f518"

    // Brukes ikke lokalt
    override val electorPath = ""
    override val appImage = "http://localhost8080:githubhash"
    override val dbJdbcUrl = "jdbc:postgresql://host.docker.internal:5433/saksbehandling?user=postgres&password=test"

    override val tokenEndpoint = "http://localhost:7165/api/v1/token"
    override val tokenIntrospectionEndpoint = "http://localhost:7165/api/v1/introspect"
    override val tokenExchangeEndpoint = "http://localhost:7165/api/v1/token/exchange"

    override val pdlScope = "localhost"
    override val pdlUrl = "http://host.docker.internal:8091/graphql"

    override val skjermingScope = "localhost"
    override val skjermingUrl = "http://host.docker.internal:8091"

    override val tiltakScope = "localhost"
    override val tiltakUrl = "http://host.docker.internal:8091"

    override val utbetalingScope = "localhost"
    override val utbetalingUrl = "http://host.docker.internal:8091"

    override val dokarkivScope = "localhost"
    override val dokarkivUrl = "http://host.docker.internal:8091"

    override val dokdistScope = "localhost"
    override val dokdistUrl = "http://host.docker.internal:8091"

    override val pdfgenUrl = "http://host.docker.internal:8081"
    override val pdfgenrsUrl = "http://host.docker.internal:8084"

    override val microsoftScope = "localhost"
    override val microsoftUrl = "host.docker.internal:8091"

    override val datadelingScope = "localhost"
    override val datadelingUrl = "http://host.docker.internal:8082"

    override val meldekortApiScope = "tiltakspenger-meldekort-api"
    override val meldekortApiUrl = "http://localhost:8083"

    override val aoKontorScope = "localhost"
    override val aoKontorUrl = "http://host.docker.internal:8091"

    override val veilarboppfolgingScope = "localhost"
    override val veilarboppfolgingUrl = "http://host.docker.internal:8091"

    override val oppgaveScope = "localhost"
    override val oppgaveUrl = "http://host.docker.internal:8091"

    override val sokosUtbetaldataScope = "localhost"
    override val sokosUtbetaldataUrl = "http://host.docker.internal:8091"

    override val tilgangsmaskinenUrl = "http://host.docker.internal:8091"
    override val tilgangsmaskinenScope = "localhost"

    override val tiltakspengerArenaUrl = "http://host.docker.internal:8091"
    override val tiltakspengerArenaScope = "localhost"

    override val safUrl = "http://host.docker.internal:8091"
    override val safScope = "localhost"

    override val kabalScope = "localhost"
    override val kabalUrl = "http://host.docker.internal:8091"

    override val arenaTiltaksdeltakerTopic = "arena.tiltaksdeltaker"
    override val kometTiltaksdeltakerTopic = "komet.tiltaksdeltaker"
    override val teamTiltakTiltaksdeltakerTopic = "teamtiltak.tiltaksdeltaker"
    override val leesahTopic = "pdl.leesah"
    override val aktorV2Topic = "pdl.aktor"
    override val identhendelseTopic = "tpts.identhendelse"
    override val tilbakekrevingTopic = "tilbake.privat-tilbakekreving-tiltakspenger"
}

data object DevConfig : EnvironmentConfig {
    override val profile = Profile.DEV
    override val httpPort = 8080
    override val logbackConfigurationFile = "logback.xml"

    override val roleSaksbehandler: String = System.getenv("ROLE_SAKSBEHANDLER")
    override val roleBeslutter: String = System.getenv("ROLE_BESLUTTER")
    override val roleDrift: String = System.getenv("ROLE_DRIFT")

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

    override val utbetalingScope = "dev-gcp:helved:utsjekk"
    override val utbetalingUrl = "http://utsjekk.helved"

    override val dokarkivScope = "dev-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv-q2.dev-fss-pub.nais.io"

    override val dokdistScope = "dev-fss:teamdokumenthandtering:dokdistfordeling"
    override val dokdistUrl = "https://dokdistfordeling.dev-fss-pub.nais.io"

    override val pdfgenUrl = "http://tiltakspenger-pdfgen"
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
}

data object ProdConfig : EnvironmentConfig {
    override val profile = Profile.PROD
    override val httpPort = 8080
    override val logbackConfigurationFile = "logback.xml"

    override val roleSaksbehandler: String = System.getenv("ROLE_SAKSBEHANDLER")
    override val roleBeslutter: String = System.getenv("ROLE_BESLUTTER")
    override val roleDrift: String = System.getenv("ROLE_DRIFT")

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

    override val utbetalingScope = "prod-gcp:helved:utsjekk"
    override val utbetalingUrl = "http://utsjekk.helved"

    override val dokarkivScope = "prod-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv.prod-fss-pub.nais.io"

    override val dokdistScope = "prod-fss:teamdokumenthandtering:dokdistfordeling"
    override val dokdistUrl = "https://dokdistfordeling.prod-fss-pub.nais.io"

    override val pdfgenUrl = "http://tiltakspenger-pdfgen"
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
}
