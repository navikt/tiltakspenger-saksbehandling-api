package no.nav.tiltakspenger.saksbehandling.lokal

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentProfile
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.TilbakekrevingshendelsePolicy
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import java.time.LocalDate
import java.time.LocalDateTime

/** Wiremock kan kjøres opp med docker-compose i metarepoet. */
private const val WIREMOCK_URL = "http://host.docker.internal:8091"

private data object LokalSaksnummerGenerator : SaksnummerGenerator {
    override fun generer(dato: LocalDate): Saksnummer = Saksnummer.genererSaknummer(dato = dato, løpenr = "1001")
}

/**
 * Konfigurasjonen for lokal kjøring, levert av [LokalKonfigurasjonsleverandør].
 * Ligger bare i lokalmodulen, så verken produksjons- eller utviklingsimaget kan starte med den.
 * `LokalMain` bruker normalt fakes i stedet for URL-ene, mens `LokalComposeMain` bruker dem mot Wiremock og de andre tjenestene i docker-compose.
 */
data object LocalConfig : EnvironmentConfig {
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
    override val dbJdbcUrl = System.getenv("DB_JDBC_URL")
        ?: "jdbc:postgresql://host.docker.internal:5433/saksbehandling?user=postgres&password=test"

    override val tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT") ?: "http://localhost:7165/api/v1/token"
    override val tokenIntrospectionEndpoint = System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT") ?: "http://localhost:7165/api/v1/introspect"
    override val tokenExchangeEndpoint = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT") ?: "http://localhost:7165/api/v1/token/exchange"

    override val pdlScope = "localhost"
    override val pdlUrl = "$WIREMOCK_URL/graphql"

    override val skjermingScope = "localhost"
    override val skjermingUrl = WIREMOCK_URL

    override val tiltakshistorikkScope = "localhost"
    override val tiltakshistorikkUrl = WIREMOCK_URL

    override val utbetalingScope = "localhost"
    override val utbetalingUrl = WIREMOCK_URL

    override val helvedSimuleringScope = "localhost"
    override val helvedSimuleringUrl = WIREMOCK_URL

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
