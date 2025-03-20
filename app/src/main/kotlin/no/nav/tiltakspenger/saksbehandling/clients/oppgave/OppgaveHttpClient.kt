package no.nav.tiltakspenger.saksbehandling.clients.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.Oppgavebehov
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
        oppgavebehov: Oppgavebehov,
    ): OppgaveId {
        val opprettOppgaveRequest = when (oppgavebehov) {
            Oppgavebehov.NY_SOKNAD -> {
                OpprettOppgaveRequest.opprettOppgaveRequestForSoknad(
                    fnr = fnr,
                    journalpostId = journalpostId,
                )
            }

            Oppgavebehov.NYTT_MELDEKORT -> {
                OpprettOppgaveRequest.opprettOppgaveRequestForMeldekort(
                    fnr = fnr,
                    journalpostId = journalpostId,
                )
            }

            else -> {
                logger.error { "Ukjent oppgavebehov for oppgave med journalpost: ${oppgavebehov.name}" }
                throw IllegalArgumentException("Ukjent oppgavebehov for oppgave med journalpost: ${oppgavebehov.name}")
            }
        }

        val callId = UUID.randomUUID()
        val oppgaveResponse = finnOppgave(journalpostId, opprettOppgaveRequest.oppgavetype, callId)
        if (oppgaveResponse.antallTreffTotalt > 0 && oppgaveResponse.oppgaver.isNotEmpty()) {
            logger.warn { "Oppgave for journalpostId: $journalpostId finnes fra før, callId: $callId" }
            return OppgaveId(oppgaveResponse.oppgaver.first().id.toString())
        }
        return opprettOppgave(opprettOppgaveRequest, callId)
    }

    override suspend fun opprettOppgaveUtenDuplikatkontroll(fnr: Fnr, oppgavebehov: Oppgavebehov): OppgaveId {
        val callId = UUID.randomUUID()
        val opprettOppgaveRequest = if (oppgavebehov == Oppgavebehov.ENDRET_TILTAKDELTAKER) {
            OpprettOppgaveRequest.opprettOppgaveRequestForEndretTiltaksdeltaker(
                fnr = fnr,
            )
        } else {
            logger.error { "Ukjent oppgavebehov for oppgave uten journalpost og duplikatkontroll: ${oppgavebehov.name}" }
            throw IllegalArgumentException("Ukjent oppgavebehov for oppgave uten journalpost og duplikatkontroll: ${oppgavebehov.name}")
        }
        return opprettOppgave(opprettOppgaveRequest, callId)
    }

    override suspend fun ferdigstillOppgave(oppgaveId: OppgaveId) {
        val callId = UUID.randomUUID()
        val oppgave = getOppgave(oppgaveId, callId)
        if (oppgave.erFerdigstilt()) {
            logger.warn { "Oppgave med id $oppgaveId er allerede ferdigstilt, callId: $callId" }
        } else {
            ferdigstillOppgave(oppgave, callId)
            logger.info { "Ferdigstilt oppgave med id $oppgaveId, callId $callId" }
        }
    }

    override suspend fun erFerdigstilt(oppgaveId: OppgaveId): Boolean {
        val callId = UUID.randomUUID()
        logger.info { "Sjekker om oppgave med id $oppgaveId er ferdigstilt, callId $callId" }
        val oppgave = getOppgave(oppgaveId, callId)
        return oppgave.erFerdigstilt()
    }

    private suspend fun opprettOppgave(
        opprettOppgaveRequest: OpprettOppgaveRequest,
        callId: UUID,
    ): OppgaveId {
        val jsonPayload = objectMapper.writeValueAsString(opprettOppgaveRequest)
        val request = createPostRequest(jsonPayload, getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 201) {
            logger.error { "Kunne ikke opprette oppgave, statuskode $status. CallId: $callId ${opprettOppgaveRequest.journalpostId?.let { ", journalpostId: $it" }}" }
            sikkerlogg.error { httpResponse.body() }
            error("Kunne ikke opprette oppgave, statuskode $status")
        }
        val jsonResponse = httpResponse.body()
        val oppgaveId = objectMapper.readValue<OpprettOppgaveResponse>(jsonResponse).id
        logger.info { "Opprettet oppgave med id $oppgaveId for callId: $callId ${opprettOppgaveRequest.journalpostId?.let { ", journalpostId: $it" }}" }
        return OppgaveId(oppgaveId.toString())
    }

    private suspend fun finnOppgave(
        journalpostId: JournalpostId,
        oppgaveType: String,
        callId: UUID,
    ): FinnOppgaveResponse {
        val request = createGetRequest(createGetOppgaveUri(journalpostId, oppgaveType), getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 200) {
            logger.error { "Noe gikk galt ved søk etter oppgave, statuskode $status. JournalpostId: $journalpostId, callId: $callId" }
            sikkerlogg.error { httpResponse.body() }
            error("Noe gikk galt ved søk etter oppgave, statuskode $status")
        }
        val jsonResponse = httpResponse.body()
        return objectMapper.readValue<FinnOppgaveResponse>(jsonResponse)
    }

    private suspend fun getOppgave(
        oppgaveId: OppgaveId,
        callId: UUID,
    ): Oppgave {
        val request = createGetRequest(URI.create("$uri/$oppgaveId"), getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 200) {
            logger.error { "Noe gikk galt ved henting av oppgave med id $oppgaveId, statuskode $status, callId: $callId" }
            sikkerlogg.error { httpResponse.body() }
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
            versjon = oppgave.versjon,
            status = OppgaveStatus.FERDIGSTILT,
        )
        val jsonPayload = objectMapper.writeValueAsString(ferdigstillOppgaveRequest)
        val request = createPatchRequest(URI.create("$uri/${oppgave.id}"), jsonPayload, getToken().token, callId)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status != 200) {
            logger.error { "Noe gikk galt ved ferdigstilling av oppgave med id ${oppgave.id}, statuskode $status, callId: $callId" }
            sikkerlogg.error { httpResponse.body() }
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

    private fun createGetOppgaveUri(journalpostId: JournalpostId, oppgaveType: String): URI {
        return URI.create("$uri?tema=$TEMA_TILTAKSPENGER&oppgavetype=$oppgaveType&journalpostId=$journalpostId&statuskategori=AAPEN")
    }
}
