package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.httpklient.tryMap
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteOppfølgingskontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighet
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighetMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorhistorikkMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.OppfølgingskontorKlient
import java.net.URI
import java.time.Clock
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [HttpKlient]-basert klient mot navkontor-APIet til Arbeidsoppfølging (GraphQL-spørringene `kontorTilhorighet` og `kontorHistorikk`).
 *
 * Kildekode: https://github.com/navikt/ao-oppfolgingskontor
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: https://ao-oppfolgingskontor.intern.dev.nav.no/sdl (GraphQL-skjema)
 * Slack: #team_dab_arbeidsoppfølging
 * Teamkatalog: https://teamkatalogen.nav.no/team/1ad2c9ea-3221-4666-93f3-fe6f7cae94ef
 *
 * `kontorTilhorighet` lar tjenesten velge kontoret: arbeidsoppfølgingskontor, deretter Arena-kontor og til slutt geografisk tilknytning.
 * Svaret er `null` når personen ikke har noe kontor.
 * `kontorHistorikk` returnerer alle innslag uten filtrering, og domenet ([Kontorhistorikk]) avgjør hvilket innslag som skal brukes til hva.
 * Historikken returneres også for historiske fødselsnumre/d-numre, som er forventet.
 *
 * Vi henter kun feltene vi har dekning for å bruke (behandlingskatalog), og ikke `registrant`/`registrantType`/`endretAv`.
 * Tjenesten krever `traceparent`-header, som OpenTelemetry-agenten (autoInstrumentation i nais.yml) legger på.
 *
 * Feillogging skjer ikke her, men i [no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService], som har domenekonteksten (loggkontekst med sakId/saksnummer/...).
 * Klienten bærer derfor httpklient sine rå typer videre til domenet: [HttpKlientError] på feilstiene og [HttpKlientMetadata] ellers.
 */
class OppfølgingskontorHttpklient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 2.seconds,
    timeout: Duration = 3.seconds,
    clock: Clock,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : OppfølgingskontorKlient {
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
    ): Either<KanIkkeHenteOppfølgingskontor, KontorTilhørighetMedMetadata> =
        spør(KONTOR_TILHØRIGHET_QUERY, fnr) { data, metadata ->
            KontorTilhørighetMedMetadata(
                kontorTilhørighet = data?.kontorTilhorighet?.toDomene(),
                httpKlientMetadata = metadata,
            )
        }

    override suspend fun hentKontorhistorikk(
        fnr: Fnr,
    ): Either<KanIkkeHenteOppfølgingskontor, KontorhistorikkMedMetadata> =
        spør(KONTORHISTORIKK_QUERY, fnr) { data, metadata ->
            KontorhistorikkMedMetadata(
                kontorhistorikk = Kontorhistorikk((data?.kontorHistorikk ?: emptyList()).map { it.toDomene() }),
                httpKlientMetadata = metadata,
            )
        }

    private suspend fun <T> spør(
        query: String,
        fnr: Fnr,
        tilDomene: (GraphQlData?, HttpKlientMetadata) -> T,
    ): Either<KanIkkeHenteOppfølgingskontor, T> {
        val request = GraphQlRequest(query = query, variables = mapOf("ident" to fnr.verdi))
        // API-et svarer alltid 200 ved suksess (også GraphQL-feil kommer med 200); alt annet skal være feil.
        val response = httpKlient.postJson<GraphQlResponse>(uri, request, godta = Statusregel.Eksakt(200)).getOrElse { error ->
            return when (error) {
                is HttpKlientError.UventetStatus -> KanIkkeHenteOppfølgingskontor.UventetHttpStatus(error)

                is HttpKlientError.RequestIkkeSendt,
                is HttpKlientError.IngenRespons,
                is HttpKlientError.DeserializationError,
                -> KanIkkeHenteOppfølgingskontor.KallFeilet(error)
            }.left()
        }
        if (!response.body.errors.isNullOrEmpty()) {
            return KanIkkeHenteOppfølgingskontor.GraphQlFeil(httpKlientMetadata = response.metadata).left()
        }
        // Body-en er gyldig JSON, men innholdet lar seg kanskje ikke mappe til domenet (f.eks. et endretTidspunkt vi ikke klarer å tolke).
        // tryMap pakker det som httpklient sin DeserializationError slik at throwable og metadata følger med til feillogging.
        return response.tryMap { body -> tilDomene(body.data, response.metadata) }
            .mapLeft { KanIkkeHenteOppfølgingskontor.KallFeilet(httpKlientError = it) }
    }
}

private val KONTOR_TILHØRIGHET_QUERY = """
    query KontorTilhorighet(${'$'}ident: String!) {
      kontorTilhorighet(ident: ${'$'}ident) {
        kontorId
        kontorNavn
        kontorType
      }
    }
""".trimIndent()

private val KONTORHISTORIKK_QUERY = """
    query Kontorhistorikk(${'$'}ident: String!) {
      kontorHistorikk(ident: ${'$'}ident) {
        kontorId
        kontorNavn
        kontorType
        endretTidspunkt
      }
    }
""".trimIndent()

private data class GraphQlRequest(
    val query: String,
    val variables: Map<String, String>,
)

/** Kun ment brukt av testene utenfor denne fila (serialiseres som fasit i `FakeHttpTransport.leggIKøJson`). */
data class GraphQlResponse(
    val data: GraphQlData? = null,
    val errors: List<Map<String, Any?>>? = null,
)

/**
 * Felles `data`-objekt for begge spørringene; bare feltet for spørringen vi sendte blir satt.
 * Kun ment brukt av testene utenfor denne fila.
 */
data class GraphQlData(
    val kontorTilhorighet: KontorTilhørighetDto? = null,
    val kontorHistorikk: List<KontorhistorikkDto>? = null,
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
data class KontorhistorikkDto(
    val kontorId: String,
    val kontorNavn: String?,
    val kontorType: KontorTypeDto,
    val endretTidspunkt: String,
) {
    fun toDomene(): Kontorhistorikkinnslag =
        Kontorhistorikkinnslag(
            kontorId = kontorId,
            kontorNavn = kontorNavn,
            kontorType = kontorType.toDomene(),
            // APIet serialiserer `ZonedDateTime.toString()` (f.eks. "2024-05-01T10:15:30+02:00[Europe/Oslo]" eller "2024-05-01T08:15:30Z[UTC]" hvis serveren kjører i UTC).
            // Vi konverterer alltid til Europe/Oslo for å få samme "vegg-klokke"-tidspunkt som resten av appen bruker, og deretter til [java.time.LocalDateTime] som domenet vårt forventer.
            endretTidspunkt = ZonedDateTime.parse(endretTidspunkt)
                .withZoneSameInstant(zoneIdOslo)
                .toLocalDateTime(),
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
