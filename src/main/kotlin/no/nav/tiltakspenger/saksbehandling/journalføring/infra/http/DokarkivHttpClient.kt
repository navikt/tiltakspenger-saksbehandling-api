package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
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
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.infra.http.httpClientWithRetry
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.infra.http.toJournalpostRequest
import no.nav.tiltakspenger.saksbehandling.klage.ports.JournalførKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.toJournalpostRequest
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.http.utgåendeJournalpostRequest
import java.time.Clock

internal const val DOKARKIV_PATH = "rest/journalpostapi/v1/journalpost"

/**
 * https://confluence.adeo.no/display/BOA/opprettJournalpost
 * swagger: https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/
 *
 * Det er caller sitt ansvar å logge errors ved exception
 */
internal class DokarkivHttpClient(
    private val baseUrl: String,
    private val client: HttpClient = httpClientWithRetry(timeout = 30L),
    private val clock: Clock,
    private val getToken: suspend () -> AccessToken,
) : JournalførRammevedtaksbrevKlient,
    JournalførMeldekortKlient,
    JournalførKlagebrevKlient {

    private val log = KotlinLogging.logger {}

    override suspend fun journalførVedtaksbrevForRammevedtak(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        val jsonBody = vedtak.utgåendeJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    override suspend fun journalførVedtaksbrevForMeldekortvedtak(
        meldekortvedtak: Meldekortvedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        val jsonBody = meldekortvedtak.toJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    override suspend fun journalførAvvisningsvedtakForKlagevedtak(
        klagevedtak: Klagevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        val jsonBody = klagevedtak.toJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    override suspend fun journalførInnstillingsbrevForOpprettholdtKlagebehandling(
        klagebehandling: Klagebehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        val jsonBody = klagebehandling.toJournalpostRequest(pdfOgJson)
        return opprettJournalpost(jsonBody, correlationId)
    }

    private suspend fun opprettJournalpost(
        jsonRequestBody: String,
        correlationId: CorrelationId,
    ): Pair<JournalpostId, JournalførBrevMetadata> {
        val token = Either.catch { getToken() }.getOrElse {
            Sikkerlogg.error(it) { "Kunne ikke hente token for journalføring. jsonBody: $jsonRequestBody, correlationId: $correlationId" }
            throw RuntimeException("Kunne ikke hente token for journalføring. Se sikkerlogg for mer kontekst.")
        }
        try {
            log.info { "Starter journalføring av dokument" }

            val res = client.post("$baseUrl/$DOKARKIV_PATH") {
                accept(ContentType.Application.Json)
                header("X-Correlation-ID", correlationId.value)
                header("Nav-Callid", correlationId.value)
                parameter("forsoekFerdigstill", true)
                bearerAuth(token.token)
                contentType(ContentType.Application.Json)
                setBody(jsonRequestBody)
            }

            when (val status = res.status) {
                HttpStatusCode.Created -> {
                    val responseBody: String = res.call.body()
                    val response = deserialize<DokarkivResponse>(responseBody)
                    log.info { "Mottok 201 Created fra dokarkiv med response body: $responseBody" }

                    val journalpostId = if (response.journalpostId.isNullOrEmpty()) {
                        log.warn { "Kallet til dokarkiv gikk ok, men vi fikk ingen journalpostId fra dokarkiv" }
                        throw IllegalStateException("Kallet til dokarkiv gikk ok, men vi fikk ingen journalpostId fra dokarkiv")
                    } else {
                        response.journalpostId
                    }

                    if ((response.journalpostferdigstilt == null) || !response.journalpostferdigstilt) {
                        log.error { "Kunne ikke ferdigstille journalføring for journalpostId: $journalpostId. response=$responseBody" }
                    }

                    log.info { "Vi har opprettet journalpost med id : $journalpostId" }
                    return JournalpostId(journalpostId) to JournalførBrevMetadata(
                        requestBody = jsonRequestBody,
                        responseStatus = "201 Created",
                        responseBody = responseBody,
                        journalføringsTidspunkt = nå(clock),
                    )
                }

                else -> {
                    log.warn { "Kallet til dokarkiv feilet $status ${status.description}" }
                    throw RuntimeException("Feil i kallet til dokarkiv")
                }
            }
        } catch (throwable: Throwable) {
            if (throwable is ResponseException) {
                val status = throwable.response.status
                if (throwable is ClientRequestException) {
                    when (status) {
                        Unauthorized, Forbidden -> {
                            log.warn(RuntimeException("Trigger stacktrace for debug.")) { "Invaliderer cache for systemtoken mot dokarkiv. status: $status." }
                            token.invaliderCache()
                        }

                        Conflict -> {
                            log.warn { "Har allerede blitt journalført (409 Conflict)" }
                            val responseBody: String = throwable.response.call.body()
                            val response = deserialize<DokarkivResponse>(responseBody)
                            return JournalpostId(response.journalpostId.orEmpty()) to JournalførBrevMetadata(
                                requestBody = jsonRequestBody,
                                responseStatus = "409 Conflict",
                                responseBody = responseBody,
                                journalføringsTidspunkt = nå(clock),
                            )
                        }

                        else -> {
                            log.warn(throwable) { "Fikk klientside-feilkode fra dokarkiv: $status." }
                        }
                    }
                } else {
                    log.warn(throwable) { "Fikk feilkode fra dokarkiv: $status." }
                }
            }
            throw throwable
        }
    }

    data class DokarkivResponse(
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
