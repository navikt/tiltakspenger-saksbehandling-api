package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.post
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorhistorikkMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.tilKlientkall
import java.net.URI
import java.time.Clock
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [HttpKlient]-basert klient mot det nye navkontor-APIet til Arbeidsoppfølging (GraphQL-spørringen `kontorHistorikk(ident: String!)`).
 *
 * Kildekode: https://github.com/navikt/ao-oppfolgingskontor
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: https://ao-oppfolgingskontor.intern.dev.nav.no/sdl (GraphQL-skjema)
 * Slack: #team_dab_arbeidsoppfølging
 * Teamkatalog: https://teamkatalogen.nav.no/team/1ad2c9ea-3221-4666-93f3-fe6f7cae94ef
 *
 * Brukes i første iterasjon for sammenligning mot gammel klient ([VeilarboppfolgingHttpClient]), og kun av [SammenligningVeilarboppfolgingKlient].
 *
 * Vi henter kun feltene vi har dekning for å bruke (behandlingskatalog), og returnerer alle innslag uten
 * å filtrere - domenet ([Kontorhistorikk]) avgjør hvilket innslag som skal brukes til hva.
 *
 * Feillogging skjer ikke her, men i [SammenligningVeilarboppfolgingKlient], som har domenekonteksten (loggkontekst med sakId/saksnummer/...).
 * Klienten bærer derfor httpklient sine rå typer videre til domenet: [HttpKlientError] på feilstiene ([KanIkkeHenteKontorhistorikk.httpKlientError]) og [no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata] ellers.
 *
 * Merk at dette APIet returnerer historikk også for historiske fødselsnumre/d-numre, som er forventet.
 * Dersom man slår på ident i responsen, vil man få identen kontornummeret ble registrert på, selvom det er historisk.
 */
class KontorhistorikkHttpklient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 2.seconds,
    timeout: Duration = 3.seconds,
    clock: Clock,
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = timeout
        this.successStatus = { it == 200 }
        this.authTokenProvider = authTokenProvider
    },
) {
    private val uri = URI.create("$baseUrl/graphql")

    suspend fun hentKontorhistorikk(
        fnr: Fnr,
    ): Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata> {
        return httpKlient.post<GraphQlResponse>(uri, lagGraphQlRequest(fnr.verdi)).mapLeft { error ->
            when (error) {
                is HttpKlientError.UventetStatus -> KanIkkeHenteKontorhistorikk.UventetHttpStatus(error)

                is HttpKlientError.RequestIkkeSendt,
                is HttpKlientError.IngenRespons,
                is HttpKlientError.DeserializationError,
                -> KanIkkeHenteKontorhistorikk.KallFeilet(error)
            }
        }.flatMap { response ->
            if (!response.body.errors.isNullOrEmpty()) {
                return@flatMap KanIkkeHenteKontorhistorikk.GraphQlFeil(httpKlientMetadata = response.metadata).left()
            }
            Either.catch {
                Kontorhistorikk((response.body.data?.kontorHistorikk ?: emptyList()).map { it.toDomene() })
            }.mapLeft { throwable ->
                // Body-en er gyldig JSON, men innholdet lot seg ikke mappe til domenet (f.eks. et
                // endretTidspunkt vi ikke klarer å tolke). Vi pakker det som httpklient sin
                // DeserializationError slik at throwable og metadata følger med til feillogging.
                KanIkkeHenteKontorhistorikk.KallFeilet(
                    httpKlientError = HttpKlientError.DeserializationError(
                        throwable = throwable,
                        body = response.rawResponseString ?: "",
                        statusCode = response.statusCode,
                        metadata = response.metadata,
                    ),
                )
            }.map { kontorhistorikk ->
                KontorhistorikkMedMetadata(
                    kontorhistorikk = kontorhistorikk,
                    kall = response.metadata.tilKlientkall(),
                    httpKlientMetadata = response.metadata,
                )
            }
        }
    }
}

private fun lagGraphQlRequest(ident: String): GraphQlRequest = GraphQlRequest(
    query = """
        query Kontorhistorikk(${'$'}ident: String!) {
          kontorHistorikk(ident: ${'$'}ident) {
            kontorId
            kontorNavn
            kontorType
            endretTidspunkt
          }
        }
    """.trimIndent(),
    variables = mapOf("ident" to ident),
)

private data class GraphQlRequest(
    val query: String,
    val variables: Map<String, String>,
)

/** Kun ment brukt av testene utenfor denne fila (konstrueres direkte i `HttpKlientFake.enqueueResponse`). */
data class GraphQlResponse(
    val data: GraphQlData? = null,
    val errors: List<Map<String, Any?>>? = null,
)

/** Kun ment brukt av testene utenfor denne fila. */
data class GraphQlData(
    val kontorHistorikk: List<KontorhistorikkDto>? = null,
)

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
            // APIet serialiserer `ZonedDateTime.toString()` (f.eks. "2024-05-01T10:15:30+02:00[Europe/Oslo]"
            // eller "2024-05-01T08:15:30Z[UTC]" hvis serveren kjører i UTC). Vi konverterer alltid til
            // Europe/Oslo for å få samme "vegg-klokke"-tidspunkt som resten av appen bruker, og deretter
            // til [LocalDateTime] som domenet vårt forventer.
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
