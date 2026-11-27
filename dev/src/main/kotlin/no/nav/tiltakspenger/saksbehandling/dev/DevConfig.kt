package no.nav.tiltakspenger.saksbehandling.dev

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentProfile
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.TilbakekrevingshendelsePolicy
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import java.time.LocalDate
import java.time.LocalDateTime

data object DevConfig : EnvironmentConfig {
    override val environmentProfile = EnvironmentProfile.ANNEN_NAIS_KLYNGE
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

    override val tiltakshistorikkScope = "dev-gcp:team-mulighetsrommet:tiltakshistorikk"
    override val tiltakshistorikkUrl = "http://tiltakshistorikk.team-mulighetsrommet"

    override val utbetalingScope = "dev-gcp:helved:utsjekk"
    override val utbetalingUrl = "http://utsjekk.helved"

    override val helvedSimuleringScope = "dev-gcp:helved:simulering"
    override val helvedSimuleringUrl = "http://simulering.helved"

    override val dokarkivScope = "dev-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv-q2.dev-fss-pub.nais.io"

    override val dokdistScope = "dev-fss:teamdokumenthandtering:dokdistfordeling"
    override val dokdistUrl = "https://dokdistfordeling.dev-fss-pub.nais.io"

    override val pdfgenrsUrl = "http://tiltakspenger-pdfgenrs"

    override val datadelingScope = "dev-gcp:tpts:tiltakspenger-datadeling"
    override val datadelingUrl = "http://tiltakspenger-datadeling"

    override val meldekortApiScope = "dev-gcp:tpts:tiltakspenger-meldekort-api"
    override val meldekortApiUrl = "http://tiltakspenger-meldekort-api"

    override val soknadApiScope = "dev-gcp:tpts:tiltakspenger-soknad-api"
    override val soknadApiUrl = "http://tiltakspenger-soknad-api"

    override val aoKontorScope = "dev-gcp.dab.ao-oppfolgingskontor"
    override val aoKontorUrl = "http://ao-oppfolgingskontor.dab"

    override val oppgaveScope = "dev-fss:oppgavehandtering:oppgave"
    override val oppgaveUrl = "https://oppgave.dev-fss-pub.nais.io"

    override val sokosUtbetaldataScope = "dev-fss:okonomi:sokos-utbetaldata"
    override val sokosUtbetaldataUrl = "https://sokos-utbetaldata.dev-fss-pub.nais.io"

    override val tilgangsmaskinenUrl = "http://populasjonstilgangskontroll.tilgangsmaskin"
    override val tilgangsmaskinenScope = "api://dev-gcp.tilgangsmaskin.populasjonstilgangskontroll/.default"

    override val entraProxyUrl = "http://entra-proxy.sikkerhetstjenesten"
    override val entraProxyScope = "api://dev-gcp.sikkerhetstjenesten.entra-proxy/.default"

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
    override val utbetalingTopic = "tpts.utbetaling-v1"
    override val helvedStatusTopic = "helved.status.v1"
    override val oppdragsversjonForNyeSaker = Oppdragsversjon.V3
    override val saksnummerGenerator: SaksnummerGenerator = DevSaksnummerGenerator

    // De to testsakene i dev ble opprettet 25.09.2026; prod følger opp alle saker.
    override val utbetalingerFølgesOppFra: LocalDateTime = LocalDateTime.of(2026, 9, 25, 0, 0)

    // Team tilbakekreving genererer testdata hos seg selv i dev og sender dem på køen.
    // Saksnumrene er ikke våre, og hendelsene kommer aldri til å kunne behandles, så de slettes for å slippe å logge dem i evig tid.
    // En hendelse vi ikke klarer å lese er derfor forventet og ikke verdt en ERROR.
    override val tilbakekrevingshendelsePolicy = TilbakekrevingshendelsePolicy(
        testsaksnummerPrefiks = "BF",
        slettHendelserMedUkjentSaksnummer = true,
        slettUleseligeHendelser = true,
        loggDeserialiseringsfeilSomError = false,
    )

    override val leesahAvroSerializablePackage = "no.nav.person.pdl.leesah"
    override val aktorV2AvroSerializablePackage = "no.nav.person.pdl.aktor.v2"

    override val saksbehandlingFrontendUrl = "https://tiltakspenger-saksbehandling.ansatt.dev.nav.no"
    override val gcpProsjektId = "tpts-dev-6211"
}

private data object DevSaksnummerGenerator : SaksnummerGenerator {
    override fun generer(dato: LocalDate): Saksnummer = Saksnummer.genererSaknummer(dato = dato, løpenr = "1001")
}
