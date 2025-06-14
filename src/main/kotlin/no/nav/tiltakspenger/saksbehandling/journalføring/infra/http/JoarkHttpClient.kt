package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
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
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.contentType
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.infra.http.httpClientWithRetry
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.toJournalpostRequest
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.http.utgåendeJournalpostRequest

internal const val JOARK_PATH = "rest/journalpostapi/v1/journalpost"

/**
 * https://confluence.adeo.no/display/BOA/opprettJournalpost
 * swagger: https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/
 */
internal class JoarkHttpClient(
    private val baseUrl: String,
    private val client: HttpClient = httpClientWithRetry(timeout = 30L),
    private val getToken: suspend () -> AccessToken,
) : JournalførRammevedtaksbrevKlient,
    JournalførMeldekortKlient {

    private val log = KotlinLogging.logger {}

    override suspend fun journalførVedtaksbrevForRammevedtak(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        val jsonBody = vedtak.utgåendeJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    override suspend fun journalførMeldekortBehandling(
        meldekortBehandling: MeldekortBehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): JournalpostId {
        val jsonBody = meldekortBehandling.toJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    private suspend fun opprettJournalpost(
        jsonBody: String,
        correlationId: CorrelationId,
    ): JournalpostId {
        val token = Either.catch { getToken() }.getOrElse {
            Sikkerlogg.error(it) { "Kunne ikke hente token for journalføring. jsonBody: $jsonBody, correlationId: $correlationId" }
            throw RuntimeException("Kunne ikke hente token for journalføring. Se sikkerlogg for mer kontekst.")
        }
        try {
            log.info { "Starter journalføring av dokument" }

            val res = client.post("$baseUrl/$JOARK_PATH") {
                accept(ContentType.Application.Json)
                header("X-Correlation-ID", correlationId.value)
                header("Nav-Callid", correlationId.value)
                parameter("forsoekFerdigstill", true)
                bearerAuth(token.token)
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }

            when (res.status) {
                HttpStatusCode.Created -> {
                    val response = res.call.body<JoarkResponse>()
                    log.info { response.toString() }

                    val journalpostId = if (response.journalpostId.isNullOrEmpty()) {
                        log.error { "Kallet til Joark gikk ok, men vi fikk ingen journalpostId fra Joark" }
                        throw IllegalStateException("Kallet til Joark gikk ok, men vi fikk ingen journalpostId fra Joark")
                    } else {
                        response.journalpostId
                    }

                    if ((response.journalpostferdigstilt == null) || (response.journalpostferdigstilt == false)) {
                        log.error { "Kunne ikke ferdigstille journalføring for journalpostId: $journalpostId. response=$response" }
                    }

                    log.info { "Vi har opprettet journalpost med id : $journalpostId" }
                    return JournalpostId(journalpostId)
                }

                else -> {
                    log.error { "Kallet til joark feilet ${res.status} ${res.status.description}" }
                    throw RuntimeException("Feil i kallet til joark")
                }
            }
        } catch (throwable: Throwable) {
            if (throwable is ClientRequestException) {
                val status = throwable.response.status
                if (status == Unauthorized || status == Forbidden) {
                    log.error(RuntimeException("Trigger stacktrace for debug.")) { "Invaliderer cache for systemtoken mot joark. status: $status." }
                    token.invaliderCache()
                }
                if (status == Conflict) {
                    log.warn { "Har allerede blitt journalført (409 Conflict)" }
                    val response = throwable.response.call.body<JoarkResponse>()
                    return JournalpostId(response.journalpostId.orEmpty())
                }
            }
            if (throwable is IllegalStateException) {
                throw throwable
            } else {
                log.error(throwable) { "Ukjent feil fra joark." }
                throw RuntimeException("Ukjent feil fra joark.")
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
