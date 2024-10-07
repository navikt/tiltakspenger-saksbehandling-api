package no.nav.tiltakspenger.vedtak.clients.joark

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.ports.JournalførMeldekortGateway
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.ports.JournalførVedtaksbrevGateway
import no.nav.tiltakspenger.vedtak.clients.httpClientWithRetry

internal const val JOARK_PATH = "rest/journalpostapi/v1/journalpost"

/**
 * https://confluence.adeo.no/display/BOA/opprettJournalpost
 * swagger: https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/
 */
internal class JoarkHttpClient(
    private val baseUrl: String,
    private val client: HttpClient = httpClientWithRetry(timeout = 30L),
    private val getAccessToken: suspend () -> AccessToken,
) : JournalførVedtaksbrevGateway, JournalførMeldekortGateway {

    private val log = KotlinLogging.logger {}

    override suspend fun journalførVedtaksbrev(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        val jsonBody = vedtak.toJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    override suspend fun journalførMeldekort(
        meldekort: Meldekort,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        val jsonBody = meldekort.toJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    private suspend fun opprettJournalpost(
        jsonBody: String,
        correlationId: CorrelationId,
    ): JournalpostId {
        try {
            log.info("Starter journalføring av dokument")
            val token = getAccessToken()
            val res = client.post("$baseUrl/$JOARK_PATH") {
                accept(ContentType.Application.Json)
                header("X-Correlation-ID", correlationId.value)
                header("Nav-Callid", correlationId.value)
                parameter("forsoekFerdigstill", true)
                bearerAuth(token.value)
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }

            when (res.status) {
                HttpStatusCode.Created -> {
                    val response = res.call.body<JoarkResponse>()
                    log.info(response.toString())

                    val journalpostId = if (response.journalpostId.isNullOrEmpty()) {
                        log.error("Kallet til Joark gikk ok, men vi fikk ingen journalpostId fra Joark")
                        throw IllegalStateException("Kallet til Joark gikk ok, men vi fikk ingen journalpostId fra Joark")
                    } else {
                        response.journalpostId
                    }

                    // if ((response.journalpostferdigstilt == null) || (response.journalpostferdigstilt == false)) {
                    //     log.error("Kunne ikke ferdigstille journalføring for journalpostId: $journalpostId. response=$response")
                    //     throw IllegalStateException("Kunne ikke ferdigstille journalføring for journalpostId: $journalpostId. response=$response")
                    // }

                    log.info("Vi har opprettet journalpost med id : $journalpostId")
                    return JournalpostId(journalpostId)
                }

                else -> {
                    log.error("Kallet til joark feilet ${res.status} ${res.status.description}")
                    throw RuntimeException("Feil i kallet til joark")
                }
            }
        } catch (throwable: Throwable) {
            if (throwable is ClientRequestException && throwable.response.status == HttpStatusCode.Conflict) {
                log.warn("Har allerede blitt journalført (409 Conflict)")
                val response = throwable.response.call.body<JoarkResponse>()
                return JournalpostId(response.journalpostId.orEmpty())
            }
            if (throwable is IllegalStateException) {
                throw throwable
            } else {
                log.error(throwable) { "Kallet til joark feilet $throwable" }
                throw RuntimeException("Feil i kallet til joark $throwable")
            }
        }
    }

    data class JoarkResponse(
        val journalpostId: String?,
        val journalpostferdigstilt: Boolean?,
        val melding: String?,
        val dokumenter: List<Dokumenter>?,
    )

    data class Dokumenter(
        val dokumentInfoId: String?,
        val tittel: String?,
    )
}