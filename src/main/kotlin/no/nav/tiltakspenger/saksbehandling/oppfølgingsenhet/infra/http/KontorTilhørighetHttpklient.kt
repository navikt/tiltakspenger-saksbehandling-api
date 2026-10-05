package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorTilhørighet
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighet
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighet.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighetKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighetMedMetadata
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [HttpKlient]-basert klient mot navkontor-APIet til Arbeidsoppfølging (GraphQL-spørringen `kontorTilhorighet(ident: String!)`).
 *
 * Kildekode: https://github.com/navikt/ao-oppfolgingskontor
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: https://ao-oppfolgingskontor.intern.dev.nav.no/sdl (GraphQL-skjema)
 * Slack: #team_dab_arbeidsoppfølging
 * Teamkatalog: https://teamkatalogen.nav.no/team/1ad2c9ea-3221-4666-93f3-fe6f7cae94ef
 *
 * Tjenesten velger selv kontoret: arbeidsoppfølgingskontor, deretter Arena-kontor og til slutt geografisk tilknytning.
 * Svaret er `null` når personen ikke har noe kontor.
 * Vi henter kun feltene vi har dekning for å bruke (behandlingskatalog), og ikke `registrant`/`registrantType`.
 * Tjenesten krever `traceparent`-header, som OpenTelemetry-agenten (autoInstrumentation i nais.yml) legger på.
 *
 * Feillogging skjer ikke her, men i [no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService], som har domenekonteksten (loggkontekst med sakId/saksnummer/...).
 * Klienten bærer derfor httpklient sine rå typer videre til domenet: [HttpKlientError] på feilstiene og [no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata] ellers.
 */
class KontorTilhørighetHttpklient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 2.seconds,
    timeout: Duration = 3.seconds,
    clock: Clock,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : KontorTilhørighetKlient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            // Fast sti uten personopplysninger (identen ligger i request-bodyen), så endepunktet kan navngis i vanlig logg.
            uriSynlighet = UriSynlighet.VanligLogg,
        ),
        transport = transport,
    )

    private val uri = URI.create("$baseUrl/graphql")

    override suspend fun hentKontorTilhørighet(
        fnr: Fnr,
    ): Either<KanIkkeHenteKontorTilhørighet, KontorTilhørighetMedMetadata> {
        // API-et svarer alltid 200 ved suksess (også GraphQL-feil kommer med 200); alt annet skal være feil.
        val response = httpKlient.postJson<GraphQlResponse>(uri, lagGraphQlRequest(fnr.verdi), godta = Statusregel.Eksakt(200)).getOrElse { error ->
            return when (error) {
                is HttpKlientError.UventetStatus -> KanIkkeHenteKontorTilhørighet.UventetHttpStatus(error)

                is HttpKlientError.RequestIkkeSendt,
                is HttpKlientError.IngenRespons,
                is HttpKlientError.DeserializationError,
                -> KanIkkeHenteKontorTilhørighet.KallFeilet(error)
            }.left()
        }
        if (!response.body.errors.isNullOrEmpty()) {
            return KanIkkeHenteKontorTilhørighet.GraphQlFeil(httpKlientMetadata = response.metadata).left()
        }
        return KontorTilhørighetMedMetadata(
            kontorTilhørighet = response.body.data?.kontorTilhorighet?.toDomene(),
            httpKlientMetadata = response.metadata,
        ).right()
    }
}

private fun lagGraphQlRequest(ident: String): GraphQlRequest = GraphQlRequest(
    query = """
        query KontorTilhorighet(${'$'}ident: String!) {
          kontorTilhorighet(ident: ${'$'}ident) {
            kontorId
            kontorNavn
            kontorType
          }
        }
    """.trimIndent(),
    variables = mapOf("ident" to ident),
)

private data class GraphQlRequest(
    val query: String,
    val variables: Map<String, String>,
)

/** Kun ment brukt av testene utenfor denne fila (serialiseres som fasit i `FakeHttpTransport.leggIKøJson`). */
data class GraphQlResponse(
    val data: GraphQlData? = null,
    val errors: List<Map<String, Any?>>? = null,
)

/** Kun ment brukt av testene utenfor denne fila. */
data class GraphQlData(
    val kontorTilhorighet: KontorTilhørighetDto? = null,
)

/** Kun ment brukt av testene utenfor denne fila. */
data class KontorTilhørighetDto(
    val kontorId: String,
    val kontorNavn: String,
    val kontorType: KontorTypeDto,
) {
    fun toDomene(): KontorTilhørighet =
        KontorTilhørighet(
            kontorId = kontorId,
            kontorNavn = kontorNavn,
            kontorType = kontorType.toDomene(),
        )
}

/** Kun ment brukt av testene utenfor denne fila. */
enum class KontorTypeDto {
    ARBEIDSOPPFOLGING,
    ARENA,
    GEOGRAFISK_TILKNYTNING,
    ;

    fun toDomene(): KontorType =
        when (this) {
            ARBEIDSOPPFOLGING -> KontorType.ARBEIDSOPPFOLGING
            ARENA -> KontorType.ARENA
            GEOGRAFISK_TILKNYTNING -> KontorType.GEOGRAFISK_TILKNYTNING
        }
}
