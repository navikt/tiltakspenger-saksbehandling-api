package no.nav.tiltakspenger.vedtak.clients.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

// swagger: https://oppgave.dev.intern.nav.no/

class OppgaveHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    connectTimeout: kotlin.time.Duration = 1.seconds,
    private val timeout: kotlin.time.Duration = 5.seconds,
) : OppgaveGateway {
    private val logger = KotlinLogging.logger {}
    private val client =
        java.net.http.HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
            .build()

    private val uri = "$baseUrl/api/v1/oppgaver"

    override suspend fun opprettOppgave(
        fnr: Fnr,
        journalpostId: JournalpostId,
    ): Int {
        val callId = UUID.randomUUID()
        val oppgaveResponse = finnOppgave(journalpostId, callId)
        if (oppgaveResponse.antallTreffTotalt > 0 && oppgaveResponse.oppgaver.isNotEmpty()) {
            logger.warn { "Oppgave for journalpostId: $journalpostId finnes fra før, callId: $callId" }
            return oppgaveResponse.oppgaver.first().id
        }
        return opprettOppgave(fnr, journalpostId, callId)
    }

    override suspend fun ferdigstillOppgave(oppgaveId: Int) {
        val callId = UUID.randomUUID()
        val oppgave = getOppgave(oppgaveId, callId)
        if (oppgave.erFerdigstilt()) {
            logger.warn { "Oppgave med id $oppgaveId er allerede ferdigstilt, callId: $callId" }
        } else {
            ferdigstillOppgave(oppgave, callId)
            logger.info { "Ferdigstilt oppgave med id $oppgaveId, callId $callId" }
        }
    }

    private suspend fun opprettOppgave(
        fnr: Fnr,
        journalpostId: JournalpostId,
        callId: UUID,
    ): Int {
        val opprettOppgaveRequest = OpprettOppgaveRequest(
            personident = fnr.verdi,
            journalpostId = journalpostId.toString(),
        )
        val jsonPayload = objectMapper.writeValueAsString(opprettOppgaveRequest)
        val request = createPostRequest(jsonPayload, getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 201) {
            logger.error { "Kunne ikke opprette oppgave, statuskode $status. JournalpostId: $journalpostId, callId: $callId" }
            error("Kunne ikke opprette oppgave, statuskode $status")
        }
        val jsonResponse = httpResponse.body()
        val oppgaveId = objectMapper.readValue<OpprettOppgaveResponse>(jsonResponse).id
        logger.info { "Opprettet oppgave med id $oppgaveId for journalpostId: $journalpostId, callId: $callId" }
        return oppgaveId
    }

    private suspend fun finnOppgave(
        journalpostId: JournalpostId,
        callId: UUID,
    ): FinnOppgaveResponse {
        val request = createGetRequest(createGetOppgaveUri(journalpostId), getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 200) {
            logger.error { "Noe gikk galt ved søk etter oppgave, statuskode $status. JournalpostId: $journalpostId, callId: $callId" }
            error("Noe gikk galt ved søk etter oppgave, statuskode $status")
        }
        val jsonResponse = httpResponse.body()
        return objectMapper.readValue<FinnOppgaveResponse>(jsonResponse)
    }

    private suspend fun getOppgave(
        oppgaveId: Int,
        callId: UUID,
    ): Oppgave {
        val request = createGetRequest(URI.create("$uri/$oppgaveId"), getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 200) {
            logger.error { "Noe gikk galt ved henting av oppgave med id $oppgaveId, statuskode $status, callId: $callId" }
            error("Noe gikk galt ved henting av oppgave, statuskode $status")
        }
        val jsonResponse = httpResponse.body()
        return objectMapper.readValue<Oppgave>(jsonResponse)
    }

    private suspend fun ferdigstillOppgave(
        oppgave: Oppgave,
        callId: UUID,
    ) {
        val ferdigstillOppgaveRequest = FerdigstillOppgaveRequest(
            versjon = oppgave.versjon + 1,
            status = OppgaveStatus.FERDIGSTILT,
        )
        val jsonPayload = objectMapper.writeValueAsString(ferdigstillOppgaveRequest)
        val request = createPatchRequest(URI.create("$uri/${oppgave.id}"), jsonPayload, getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 200) {
            logger.error { "Noe gikk galt ved ferdigstilling av oppgave med id ${oppgave.id}, statuskode $status, callId: $callId" }
            error("Noe gikk galt ved ferdigstilling av oppgave, statuskode $status")
        }
    }

    private fun createPostRequest(
        jsonPayload: String,
        token: String,
        callId: UUID,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(URI.create(uri))
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Correlation-ID", callId.toString())
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }

    private fun createGetRequest(
        uri: URI,
        token: String,
        callId: UUID,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Correlation-ID", callId.toString())
            .GET()
            .build()
    }

    private fun createPatchRequest(
        uri: URI,
        jsonPayload: String,
        token: String,
        callId: UUID,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Correlation-ID", callId.toString())
            .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }

    private fun createGetOppgaveUri(journalpostId: JournalpostId): URI {
        return URI.create("$uri?tema=$TEMA_TILTAKSPENGER&oppgavetype=$OPPGAVETYPE_BEHANDLE_SAK&journalpostId=$journalpostId&statuskategori=AAPEN")
    }
}
