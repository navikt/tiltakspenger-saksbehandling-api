package no.nav.tiltakspenger.saksbehandling.infra.setup

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.tiltakspenger.libs.auth.core.AdRolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

private const val APPLICATION_NAME = "tiltakspenger-saksbehandling-api"
const val KAFKA_CONSUMER_GROUP_ID = "$APPLICATION_NAME-consumer"

const val AUTOMATISK_SAKSBEHANDLER_ID = "tp-sak"

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

object Configuration {
    private val defaultProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8080.toString(),
            "SERVICEUSER_TPTS_USERNAME" to System.getenv("SERVICEUSER_TPTS_USERNAME"),
            "SERVICEUSER_TPTS_PASSWORD" to System.getenv("SERVICEUSER_TPTS_PASSWORD"),
            "ROLE_SAKSBEHANDLER" to System.getenv("ROLE_SAKSBEHANDLER"),
            "ROLE_BESLUTTER" to System.getenv("ROLE_BESLUTTER"),
            "ROLE_FORTROLIG" to System.getenv("ROLE_FORTROLIG"),
            "ROLE_STRENGT_FORTROLIG" to System.getenv("ROLE_STRENGT_FORTROLIG"),
            "ROLE_SKJERMING" to System.getenv("ROLE_SKJERMING"),
            "ROLE_DRIFT" to System.getenv("ROLE_DRIFT"),
            "logback.configurationFile" to "logback.xml",
            "ELECTOR_PATH" to System.getenv("ELECTOR_PATH"),
            "NAIS_APP_IMAGE" to System.getenv("NAIS_APP_IMAGE"),
            "DB_JDBC_URL" to System.getenv("DB_JDBC_URL"),
            "NAIS_TOKEN_ENDPOINT" to System.getenv("NAIS_TOKEN_ENDPOINT"),
            "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
            "NAIS_TOKEN_EXCHANGE_ENDPOINT" to System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
        ),
    )

    private val localProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.LOCAL.toString(),
                "logback.configurationFile" to "logback.local.xml",
                "ROLE_SAKSBEHANDLER" to "1b3a2c4d-d620-4fcf-a29b-a6cdadf29680",
                "ROLE_BESLUTTER" to "79985315-b2de-40b8-a740-9510796993c6",
                "ROLE_FORTROLIG" to "ea930b6b-9397-44d9-b9e6-f4cf527a632a",
                "ROLE_STRENGT_FORTROLIG" to "5ef775f2-61f8-4283-bf3d-8d03f428aa14",
                "ROLE_SKJERMING" to "dbe4ad45-320b-4e9a-aaa1-73cca4ee124d",
                "ROLE_DRIFT" to "c511113e-5b22-49e7-b9c4-eeb23b01f518",
                "PDL_SCOPE" to "localhost",
                "PDL_ENDPOINT_URL" to "http://host.docker.internal:8091/graphql",
                "SKJERMING_SCOPE" to "localhost",
                "SKJERMING_URL" to "http://host.docker.internal:8091",
                "TILTAK_SCOPE" to "localhost",
                "TILTAK_URL" to "http://host.docker.internal:8091",
                "UTBETALING_SCOPE" to "localhost",
                "UTBETALING_URL" to "http://host.docker.internal:8091",
                "JOARK_SCOPE" to "localhost",
                "JOARK_URL" to "http://host.docker.internal:8091",
                "DOKDIST_SCOPE" to "localhost",
                "DOKDIST_URL" to "http://host.docker.internal:8091",
                "PDFGEN_SCOPE" to "localhost",
                "PDFGEN_URL" to "http://host.docker.internal:8081",
                "NAIS_APP_IMAGE" to "http://localhost8080:githubhash",
                "DB_JDBC_URL" to "jdbc:postgresql://host.docker.internal:5433/saksbehandling?user=postgres&password=test",
                "MICROSOFT_SCOPE" to "localhost",
                "MICROSOFT_URL" to "host.docker.internal:8091",
                "DATADELING_SCOPE" to "localhost",
                "DATADELING_URL" to "http://host.docker.internal:8082",
                "MELDEKORT_API_SCOPE" to "tiltakspenger-meldekort-api",
                "MELDEKORT_API_URL" to "http://localhost:8083",
                "VEILARBOPPFOLGING_SCOPE" to "localhost",
                "VEILARBOPPFOLGING_URL" to "http://host.docker.internal:8091",
                "OPPGAVE_SCOPE" to "localhost",
                "OPPGAVE_URL" to "http://host.docker.internal:8091",
                "ARENA_TILTAKSDELTAKER_TOPIC" to "arena.tiltaksdeltaker",
                "KOMET_TILTAKSDELTAKER_TOPIC" to "komet.tiltaksdeltaker",
                "LEESAH_TOPIC" to "pdl.leesah",
                "AKTOR_V2_TOPIC" to "pdl.aktor",
                "IDENTHENDELSE_TOPIC" to "tpts.identhendelse",
                "SOKOS_UTBETALDATA_SCOPE" to "localhost",
                "SOKOS_UTBETALDATA_URL" to "http://host.docker.internal:8091",
                "BRUK_FAKE_MELDEKORT_API" to "true",
                "NAIS_TOKEN_ENDPOINT" to "http://localhost:7165/api/v1/token",
                "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to "http://localhost:7165/api/v1/introspect",
                "NAIS_TOKEN_EXCHANGE_ENDPOINT" to "http://localhost:7165/api/v1/token/exchange",
                "TILGANGSMASKINEN_URL" to "http://host.docker.internal:8091",
                "TILGANGSMASKINEN_SCOPE" to "localhost",
            ),
        )

    private val devProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.DEV.toString(),
                "PDL_SCOPE" to "dev-fss:pdl:pdl-api",
                "PDL_ENDPOINT_URL" to "https://pdl-api.dev-fss-pub.nais.io/graphql",
                "SKJERMING_SCOPE" to "dev-gcp:nom:skjermede-personer-pip",
                "SKJERMING_URL" to "https://skjermede-personer-pip.intern.dev.nav.no",
                "TILTAK_SCOPE" to "dev-gcp:tpts:tiltakspenger-tiltak",
                "TILTAK_URL" to "http://tiltakspenger-tiltak",
                "UTBETALING_SCOPE" to "dev-gcp:helved:utsjekk",
                "UTBETALING_URL" to "http://utsjekk.helved",
                "JOARK_SCOPE" to "dev-fss:teamdokumenthandtering:dokarkiv",
                "JOARK_URL" to "https://dokarkiv-q2.dev-fss-pub.nais.io",
                "DOKDIST_SCOPE" to "dev-fss:teamdokumenthandtering:dokdistfordeling",
                "DOKDIST_URL" to "https://dokdistfordeling.dev-fss-pub.nais.io",
                "PDFGEN_SCOPE" to "dev-gcp:tpts:tiltakspenger-pdfgen",
                "PDFGEN_URL" to "http://tiltakspenger-pdfgen",
                "MICROSOFT_SCOPE" to "https://graph.microsoft.com/.default",
                "MICROSOFT_URL" to "graph.microsoft.com/v1.0",
                "DATADELING_SCOPE" to "dev-gcp:tpts:tiltakspenger-datadeling",
                "DATADELING_URL" to "http://tiltakspenger-datadeling",
                "MELDEKORT_API_SCOPE" to "dev-gcp:tpts:tiltakspenger-meldekort-api",
                "MELDEKORT_API_URL" to "http://tiltakspenger-meldekort-api",
                "VEILARBOPPFOLGING_SCOPE" to "dev-gcp:poao:veilarboppfolging",
                "VEILARBOPPFOLGING_URL" to "http://veilarboppfolging.poao",
                "OPPGAVE_SCOPE" to "dev-fss:oppgavehandtering:oppgave",
                "OPPGAVE_URL" to "https://oppgave.dev-fss-pub.nais.io",
                "ARENA_TILTAKSDELTAKER_TOPIC" to "teamarenanais.aapen-arena-tiltakdeltakerendret-v1-q2",
                "KOMET_TILTAKSDELTAKER_TOPIC" to "amt.deltaker-v1",
                "LEESAH_TOPIC" to "pdl.leesah-v1",
                "AKTOR_V2_TOPIC" to "pdl.aktor-v2",
                "IDENTHENDELSE_TOPIC" to "tpts.identhendelse-v1",
                "SOKOS_UTBETALDATA_SCOPE" to "dev-fss:okonomi:sokos-utbetaldata",
                "SOKOS_UTBETALDATA_URL" to "https://sokos-utbetaldata.dev-fss-pub.nais.io",
                "TILGANGSMASKINEN_URL" to "http://populasjonstilgangskontroll.tilgangsmaskin",
                "TILGANGSMASKINEN_SCOPE" to "api://dev-gcp.tilgangsmaskin.populasjonstilgangskontroll/.default",
            ),
        )
    private val prodProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.PROD.toString(),
                "PDL_SCOPE" to "prod-fss:pdl:pdl-api",
                "PDL_ENDPOINT_URL" to "https://pdl-api.prod-fss-pub.nais.io/graphql",
                "SKJERMING_SCOPE" to "prod-gcp:nom:skjermede-personer-pip",
                "SKJERMING_URL" to "https://skjermede-personer-pip.intern.nav.no",
                "TILTAK_SCOPE" to "prod-gcp:tpts:tiltakspenger-tiltak",
                "TILTAK_URL" to "http://tiltakspenger-tiltak",
                "UTBETALING_SCOPE" to "prod-gcp:helved:utsjekk",
                "UTBETALING_URL" to "http://utsjekk.helved",
                "JOARK_SCOPE" to "prod-fss:teamdokumenthandtering:dokarkiv",
                "JOARK_URL" to "https://dokarkiv.prod-fss-pub.nais.io",
                "DOKDIST_SCOPE" to "prod-fss:teamdokumenthandtering:dokdistfordeling",
                "DOKDIST_URL" to "https://dokdistfordeling.prod-fss-pub.nais.io",
                "PDFGEN_SCOPE" to "prod-gcp:tpts:tiltakspenger-pdfgen",
                "PDFGEN_URL" to "http://tiltakspenger-pdfgen",
                "MICROSOFT_SCOPE" to "https://graph.microsoft.com/.default",
                "MICROSOFT_URL" to "graph.microsoft.com/v1.0",
                "DATADELING_SCOPE" to "prod-gcp:tpts:tiltakspenger-datadeling",
                "DATADELING_URL" to "http://tiltakspenger-datadeling",
                "MELDEKORT_API_SCOPE" to "prod-gcp:tpts:tiltakspenger-meldekort-api",
                "MELDEKORT_API_URL" to "http://tiltakspenger-meldekort-api",
                "VEILARBOPPFOLGING_SCOPE" to "prod-gcp:poao:veilarboppfolging",
                "VEILARBOPPFOLGING_URL" to "http://veilarboppfolging.poao",
                "OPPGAVE_SCOPE" to "prod-fss:oppgavehandtering:oppgave",
                "OPPGAVE_URL" to "https://oppgave.prod-fss-pub.nais.io",
                "ARENA_TILTAKSDELTAKER_TOPIC" to "teamarenanais.aapen-arena-tiltakdeltakerendret-v1-p",
                "KOMET_TILTAKSDELTAKER_TOPIC" to "amt.deltaker-v1",
                "LEESAH_TOPIC" to "pdl.leesah-v1",
                "AKTOR_V2_TOPIC" to "pdl.aktor-v2",
                "IDENTHENDELSE_TOPIC" to "tpts.identhendelse-v1",
                "SOKOS_UTBETALDATA_SCOPE" to "prod-fss:okonomi:sokos-utbetaldata",
                "SOKOS_UTBETALDATA_URL" to "https://sokos-utbetaldata.prod-fss-pub.nais.io",
                "TILGANGSMASKINEN_URL" to "http://populasjonstilgangskontroll.tilgangsmaskin",
                "TILGANGSMASKINEN_SCOPE" to "api://prod-gcp.tilgangsmaskin.populasjonstilgangskontroll/.default",
            ),
        )

    private fun config() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" ->
                systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties

            "prod-gcp" ->
                systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties

            else -> {
                systemProperties() overriding EnvironmentVariables overriding localProperties overriding defaultProperties
            }
        }

    fun applicationProfile() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" -> Profile.DEV
            "prod-gcp" -> Profile.PROD
            else -> Profile.LOCAL
        }

    fun alleAdRoller(): List<AdRolle> =
        listOf(
            AdRolle(Saksbehandlerrolle.SAKSBEHANDLER, config()[Key("ROLE_SAKSBEHANDLER", stringType)]),
            AdRolle(Saksbehandlerrolle.BESLUTTER, config()[Key("ROLE_BESLUTTER", stringType)]),
            AdRolle(Saksbehandlerrolle.FORTROLIG_ADRESSE, config()[Key("ROLE_FORTROLIG", stringType)]),
            AdRolle(
                Saksbehandlerrolle.STRENGT_FORTROLIG_ADRESSE,
                config()[Key("ROLE_STRENGT_FORTROLIG", stringType)],
            ),
            AdRolle(Saksbehandlerrolle.SKJERMING, config()[Key("ROLE_SKJERMING", stringType)]),
            AdRolle(Saksbehandlerrolle.DRIFT, config()[Key("ROLE_DRIFT", stringType)]),
        )

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    val naisTokenIntrospectionEndpoint: String by lazy { config()[Key("NAIS_TOKEN_INTROSPECTION_ENDPOINT", stringType)] }
    val naisTokenEndpoint: String by lazy { config()[Key("NAIS_TOKEN_ENDPOINT", stringType)] }
    val tokenExchangeEndpoint: String by lazy { config()[Key("NAIS_TOKEN_EXCHANGE_ENDPOINT", stringType)] }

    val pdlScope: String by lazy { config()[Key("PDL_SCOPE", stringType)] }
    val skjermingScope: String by lazy { config()[Key("SKJERMING_SCOPE", stringType)] }
    val tiltakScope: String by lazy { config()[Key("TILTAK_SCOPE", stringType)] }
    val joarkScope: String by lazy { config()[Key("JOARK_SCOPE", stringType)] }
    val dokdistScope: String by lazy { config()[Key("DOKDIST_SCOPE", stringType)] }
    val utbetalingScope: String by lazy { config()[Key("UTBETALING_SCOPE", stringType)] }
    val microsoftScope: String by lazy { config()[Key("MICROSOFT_SCOPE", stringType)] }
    val datadelingScope: String by lazy { config()[Key("DATADELING_SCOPE", stringType)] }
    val meldekortApiScope: String by lazy { config()[Key("MELDEKORT_API_SCOPE", stringType)] }
    val veilarboppfolgingScope: String by lazy { config()[Key("VEILARBOPPFOLGING_SCOPE", stringType)] }
    val oppgaveScope: String by lazy { config()[Key("OPPGAVE_SCOPE", stringType)] }
    val sokosUtbetaldataScope: String by lazy { config()[Key("SOKOS_UTBETALDATA_SCOPE", stringType)] }

    val pdlUrl by lazy { config()[Key("PDL_ENDPOINT_URL", stringType)] }
    val skjermingUrl: String by lazy { config()[Key("SKJERMING_URL", stringType)] }
    val tiltakUrl: String by lazy { config()[Key("TILTAK_URL", stringType)] }
    val joarkUrl: String by lazy { config()[Key("JOARK_URL", stringType)] }
    val dokdistUrl: String by lazy { config()[Key("DOKDIST_URL", stringType)] }
    val pdfgenUrl: String by lazy { config()[Key("PDFGEN_URL", stringType)] }
    val utbetalingUrl: String by lazy { config()[Key("UTBETALING_URL", stringType)] }
    val microsoftUrl: String by lazy { config()[Key("MICROSOFT_URL", stringType)] }
    val datadelingUrl: String by lazy { config()[Key("DATADELING_URL", stringType)] }
    val meldekortApiUrl: String by lazy { config()[Key("MELDEKORT_API_URL", stringType)] }
    val veilarboppfolgingUrl: String by lazy { config()[Key("VEILARBOPPFOLGING_URL", stringType)] }
    val oppgaveUrl: String by lazy { config()[Key("OPPGAVE_URL", stringType)] }
    val sokosUtbetaldataUrl: String by lazy { config()[Key("SOKOS_UTBETALDATA_URL", stringType)] }

    val tilgangsmaskinenUrl: String by lazy { config()[Key("TILGANGSMASKINEN_URL", stringType)] }
    val tilgangsmaskinenScope: String by lazy { config()[Key("TILGANGSMASKINEN_SCOPE", stringType)] }

    val arenaTiltaksdeltakerTopic: String by lazy { config()[Key("ARENA_TILTAKSDELTAKER_TOPIC", stringType)] }
    val kometTiltaksdeltakerTopic: String by lazy { config()[Key("KOMET_TILTAKSDELTAKER_TOPIC", stringType)] }
    val leesahTopic: String by lazy { config()[Key("LEESAH_TOPIC", stringType)] }
    val aktorV2Topic: String by lazy { config()[Key("AKTOR_V2_TOPIC", stringType)] }
    val identhendelseTopic: String by lazy { config()[Key("IDENTHENDELSE_TOPIC", stringType)] }

    val brukFakeMeldekortApiLokalt: Boolean = config().getOrNull(Key("BRUK_FAKE_MELDEKORT_API", stringType))?.toBooleanStrictOrNull() ?: true

    fun httpPort() = config()[Key("application.httpPort", intType)]

    fun isNais() = applicationProfile() != Profile.LOCAL

    fun isProd() = applicationProfile() == Profile.PROD
    fun isDev() = applicationProfile() == Profile.DEV

    fun electorPath(): String = config()[Key("ELECTOR_PATH", stringType)]

    fun gitHash(): String = config()[Key("NAIS_APP_IMAGE", stringType)].substringAfterLast(":")

    data class DataBaseConf(
        val url: String,
    )

    fun database() = DataBaseConf(
        url = config()[Key("DB_JDBC_URL", stringType)],
    )
}
