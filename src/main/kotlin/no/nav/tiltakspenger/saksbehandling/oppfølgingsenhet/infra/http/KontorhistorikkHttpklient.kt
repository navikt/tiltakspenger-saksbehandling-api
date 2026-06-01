package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Klientkall
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorhistorikkKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorhistorikkMedMetadata
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Java HttpClient-basert implementasjon av [KontorhistorikkKlient].
 *
 * Spør det nye navkontor-APIet til Arbeidsoppfølging med GraphQL-spørringen `kontorHistorikk(ident: String!)`.
 * Vi henter kun feltene vi har dekning for å bruke (behandlingskatalog), og returnerer alle innslag uten å filtrere - domenet([Kontorhistorikk]) avgjør hvilket innslag som skal brukes til hva.
 *
 * Siden dette er første gang vi bruker APIet logger vi litt rikt:
 * - Vanlig logg (uten persondata): metadata fra konsument (sakId/saksnummer/...), uri, status og en
 *   henvisning til sikkerlogg for detaljer.
 * - Sikkerlogg: i tillegg request- og responsbody (som inneholder fnr/kontorhistorikk).
 *
 * Vi logger debug ved suksess og error ved feil. Bearer-token logges aldri.
 *
 * API github: https://github.com/navikt/ao-oppfolgingskontor
 * API skjema: https://ao-oppfolgingskontor.intern.dev.nav.no/sdl
 * Merk at dette APIet returnerer historikk også for historiske fødselsnumre/d-numre, som er forventet. Dersom man slår på ident i responsen, vil man få identen kontornummeret ble registrert på, selvom det er historisk.
 */
class KontorhistorikkHttpklient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    connectTimeout: kotlin.time.Duration = 2.seconds,
    private val timeout: kotlin.time.Duration = 3.seconds,
) : KontorhistorikkKlient {
    private val logger = KotlinLogging.logger {}
    private val client =
        java.net.http.HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build()

    private val uri = URI.create("$baseUrl/graphql")

    override suspend fun hentKontorhistorikk(
        fnr: Fnr,
        sakId: String?,
        saksnummer: String?,
        rammebehandlingId: String?,
        meldekortbehandlingId: String?,
    ): Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata> {
        val kontekst = lagKontekst(sakId, saksnummer, rammebehandlingId, meldekortbehandlingId)
        return withContext(Dispatchers.IO) {
            val payload = lagGraphQlPayload(fnr.verdi)
            Either.catch {
                val request = createRequest(payload, getToken().token)
                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
                val status = httpResponse.statusCode()
                val jsonResponse = httpResponse.body()
                val kall = Klientkall(request = payload, response = jsonResponse, httpStatus = status)

                if (status != 200) {
                    logger.error(RuntimeException("Trigger stacktrace for enklere debug")) {
                        "Feil ved kall til kontorhistorikk-API (status ulik 200). $kontekst. Status: $status. uri: $uri. Se sikkerlogg for detaljer."
                    }
                    Sikkerlogg.error { "Feil ved kall til kontorhistorikk-API. $kontekst. Status: $status. uri: $uri. Request: $payload. Response: $jsonResponse." }
                    return@withContext KanIkkeHenteKontorhistorikk.UventetHttpStatus(status, kall).left()
                }

                val parsed = objectMapper.readValue<GraphQlResponse>(jsonResponse!!)
                parsed.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
                    logger.error(RuntimeException("Trigger stacktrace for enklere debug")) {
                        "GraphQL-feil ved henting av kontorhistorikk. $kontekst. Status: $status. uri: $uri. Se sikkerlogg for detaljer."
                    }
                    Sikkerlogg.error { "GraphQL-feil ved henting av kontorhistorikk. $kontekst. Status: $status. uri: $uri. Request: $payload. Response: $jsonResponse. Errors: $errors." }
                    return@withContext KanIkkeHenteKontorhistorikk.GraphQlFeil(kall).left()
                }

                val dtos = parsed.data?.kontorHistorikk ?: emptyList()

                val kontorhistorikk = Kontorhistorikk(dtos.map { it.toDomene() })
                logger.debug { "Kall til kontorhistorikk-API OK. $kontekst. Status: $status. uri: $uri. Se sikkerlogg for detaljer." }
                Sikkerlogg.debug { "Kall til kontorhistorikk-API OK. $kontekst. Status: $status. uri: $uri. Request: $payload. Response: $jsonResponse." }
                KontorhistorikkMedMetadata(kontorhistorikk = kontorhistorikk, kall = kall)
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                // Vi logger throwable kun til sikkerlogg fordi den f.eks. fra Jackson kan inneholde
                // utdrag av responsbody (med persondata). I vanlig logg legger vi på en egen exception for å få stacktrace uten å lekke innhold.
                logger.error(RuntimeException("Trigger stacktrace for enklere debug")) {
                    "Ukjent feil ved kall til kontorhistorikk-API. $kontekst. uri: $uri. Se sikkerlogg for detaljer."
                }
                Sikkerlogg.error(it) { "Feil ved kall til kontorhistorikk-API. $kontekst. uri: $uri. Request: $payload." }
                KanIkkeHenteKontorhistorikk.KallFeilet(
                    kall = Klientkall(request = payload, response = null, httpStatus = null),
                )
            }
        }
    }

    private fun createRequest(
        jsonPayload: String,
        token: String,
    ): HttpRequest =
        HttpRequest.newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
}

private fun lagKontekst(
    sakId: String?,
    saksnummer: String?,
    rammebehandlingId: String?,
    meldekortbehandlingId: String?,
): String {
    return listOfNotNull(
        sakId?.let { "sakId=$it" },
        saksnummer?.let { "saksnummer=$it" },
        rammebehandlingId?.let { "rammebehandlingId=$it" },
        meldekortbehandlingId?.let { "meldekortbehandlingId=$it" },
    ).ifEmpty { listOf("ingen kontekst") }.joinToString(", ")
}

private fun lagGraphQlPayload(ident: String): String {
    val query =
        """
        query Kontorhistorikk(${'$'}ident: String!) {
          kontorHistorikk(ident: ${'$'}ident) {
            kontorId
            kontorNavn
            kontorType
            endretTidspunkt
          }
        }
        """.trimIndent()
    return objectMapper.writeValueAsString(
        mapOf(
            "query" to query,
            "variables" to mapOf("ident" to ident),
        ),
    )
}

private data class GraphQlResponse(
    val data: GraphQlData? = null,
    val errors: List<Map<String, Any?>>? = null,
)

private data class GraphQlData(
    val kontorHistorikk: List<KontorhistorikkDto>? = null,
)

private data class KontorhistorikkDto(
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

private enum class KontorTypeDto {
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
