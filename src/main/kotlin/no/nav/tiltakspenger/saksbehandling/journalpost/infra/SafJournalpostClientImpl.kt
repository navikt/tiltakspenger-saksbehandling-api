package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.infra.graphql.GraphQLResponse
import no.nav.tiltakspenger.saksbehandling.infra.http.httpClientWithRetry
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import tools.jackson.module.kotlin.readValue

/**
 * Dokumentasjon for SAF
 * https://confluence.adeo.no/x/fY5zEg
 */
class SafJournalpostClientImpl(
    private val httpClient: HttpClient = httpClientWithRetry(timeout = 60L),
    private val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
) : SafJournalpostClient {
    private val log = KotlinLogging.logger {}
    private val journalPostQuery =
        SafJournalpostClient::class
            .java
            .getResource("/saf/hentJournalpost.graphql")!!
            .readText()
            .replace(Regex("[\n\t]"), "")

    override suspend fun hentJournalpost(
        journalpostId: JournalpostId,
    ): Journalpost? {
        val accessToken = getToken().token

        val findJournalpostRequest =
            FindJournalpostRequest(
                query = journalPostQuery,
                variables = Variables(journalpostId.toString()),
            )

        val httpResponse = try {
            httpClient
                .post("$baseUrl/graphql") {
                    setBody(findJournalpostRequest)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
                        append("X-Correlation-ID", journalpostId.toString())
                        append(HttpHeaders.ContentType, "application/json")
                    }
                }
        } catch (e: Exception) {
            if (e is ResponseException) {
                val status = e.response.status
                val responsebody = e.response.bodyAsText()
                log.error { "Noe gikk galt ved kall til SAF for journalpostId $journalpostId: feilkode: $status, melding: $responsebody" }
            } else {
                log.error { "Noe gikk galt ved kall til SAF for journalpostId $journalpostId: feilmelding: ${e.message}" }
            }
            throw RuntimeException("Noe gikk galt ved kall til SAF")
        }

        val hentJournalpostResponse =
            objectMapper.readValue<GraphQLResponse<HentJournalpostResponse>?>(httpResponse.bodyAsText())

        if (hentJournalpostResponse == null) {
            log.error { "Kall til SAF feilet for $journalpostId" }
            return null
        }

        if (hentJournalpostResponse.errors != null) {
            /*
             Fra dokumentasjonen:
             Siden GraphQL av design returnerer 200 OK på alle svar så er det et behov for å kunne skille tekniske og funksjonelle feil fra kall som går OK.
             Noen saf queries støtter feilkoder i graphql svaret: forbidden, not_found,  bad_request, server_error
             @link https://confluence.adeo.no/spaces/BOA/pages/309563246/saf+-+Utviklerveiledning#safUtviklerveiledning-Feilh%C3%A5ndtering
             */
            hentJournalpostResponse.errors.forEach {
                if (it.extensions?.code == "not_found" || it.extensions?.code == "bad_request") {
                    log.warn { "Saf returnerte feilmelding: $it" }
                } else {
                    log.error { "Saf returnerte feilmelding: $it" }
                }
            }
            return null
        }

        if (hentJournalpostResponse.data?.journalpost?.datoOpprettet == null) {
            log.error { "Klarte ikke hente data fra SAF $journalpostId" }
            return null
        }

        return hentJournalpostResponse.data.journalpost
    }
}

data class FindJournalpostRequest(val query: String, val variables: Variables)

data class Variables(val id: String)

data class HentJournalpostResponse(
    val journalpost: Journalpost?,
)
