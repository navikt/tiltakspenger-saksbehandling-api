package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.get
import no.nav.tiltakspenger.libs.httpklient.patch
import no.nav.tiltakspenger.libs.httpklient.post
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import java.net.URI
import java.time.Clock
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Sender oppgaver til oppgavesystemet (vises i Gosys)
 *
 * Kildekode: https://github.com/navikt/oppgave
 * Dokumentasjon: https://confluence.adeo.no/spaces/BOA/pages/791031394/dokarkiv+tjenesteoversikt og https://kodeverk-web.intern.nav.no/kodeverk/Oppgavetyper
 * API-spec: https://oppgave.intern.dev.nav.no/ (Swagger) og https://oppgave.intern.dev.nav.no/api/openapi.yaml (Spec)
 * Slack: #team-oppgavehåndtering
 * Teamkatalog: https://teamkatalogen.nav.no/team/1672d05d-46ed-4406-a3a4-8343db75c285
 */
class OppgaveHttpClient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 2.seconds,
    private val timeout: Duration = 5.seconds,
    private val clock: Clock,
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = timeout
        this.successStatus = { it == 200 }
        this.authTokenProvider = authTokenProvider
    },
) : OppgaveKlient {
    private val logger = KotlinLogging.logger {}

    private val oppgaverUri = URI.create("$baseUrl/api/v1/oppgaver")

    override suspend fun opprettOppgave(
        fnr: Fnr,
        journalpostId: JournalpostId,
        oppgavebehov: Oppgavebehov,
    ): Either<HttpKlientError, OppgaveId> {
        val opprettOppgaveRequest = when (oppgavebehov) {
            Oppgavebehov.NYTT_MELDEKORT -> {
                OpprettOppgaveRequest.opprettOppgaveRequestForMeldekort(
                    fnr = fnr,
                    journalpostId = journalpostId,
                    clock = clock,
                )
            }

            Oppgavebehov.NY_SOKNAD -> {
                OpprettOppgaveRequest.opprettOppgaveRequestForSoknad(
                    fnr = fnr,
                    journalpostId = journalpostId,
                    clock = clock,
                )
            }

            Oppgavebehov.ENDRET_TILTAKDELTAKER,
            Oppgavebehov.FATT_BARN,
            Oppgavebehov.DOED,
            Oppgavebehov.ADRESSEBESKYTTELSE,
            -> throw IllegalArgumentException("Oppgavebehov ${oppgavebehov.name} har ikke journalpost - bruk opprettOppgaveUtenDuplikatkontroll")
        }

        val callId = UUID.randomUUID()
        return finnOppgave(journalpostId, opprettOppgaveRequest.oppgavetype, callId).flatMap { oppgaveResponse ->
            if (oppgaveResponse.antallTreffTotalt > 0 && oppgaveResponse.oppgaver.isNotEmpty()) {
                logger.warn { "Oppgave for journalpostId: $journalpostId finnes fra før, callId: $callId" }
                OppgaveId(oppgaveResponse.oppgaver.first().id.toString()).right()
            } else {
                opprettOppgave(opprettOppgaveRequest, callId)
            }
        }
    }

    override suspend fun opprettOppgaveUtenDuplikatkontroll(
        fnr: Fnr,
        oppgavebehov: Oppgavebehov,
        tilleggstekst: String?,
    ): Either<HttpKlientError, OppgaveId> {
        val callId = UUID.randomUUID()
        val opprettOppgaveRequest = when (oppgavebehov) {
            Oppgavebehov.ENDRET_TILTAKDELTAKER -> OpprettOppgaveRequest.opprettOppgaveRequestForEndretTiltaksdeltaker(
                fnr,
                tilleggstekst,
                clock = clock,
            )

            Oppgavebehov.FATT_BARN -> OpprettOppgaveRequest.opprettOppgaveRequestForFattBarn(fnr, clock = clock)

            Oppgavebehov.DOED -> OpprettOppgaveRequest.opprettOppgaveRequestForDoedsfall(fnr, clock = clock)

            Oppgavebehov.ADRESSEBESKYTTELSE -> OpprettOppgaveRequest.opprettOppgaveRequestForAdressebeskyttelse(fnr, clock = clock)

            Oppgavebehov.NYTT_MELDEKORT,
            Oppgavebehov.NY_SOKNAD,
            -> throw IllegalArgumentException("Oppgavebehov ${oppgavebehov.name} skal ha duplikatkontroll på journalpost - bruk opprettOppgave")
        }
        return opprettOppgave(opprettOppgaveRequest, callId)
    }

    override suspend fun ferdigstillOppgave(oppgaveId: OppgaveId): Either<HttpKlientError, Unit> {
        val callId = UUID.randomUUID()
        return getOppgave(oppgaveId, callId).flatMap { oppgave ->
            if (oppgave.erFerdigstilt()) {
                logger.warn { "Oppgave med id $oppgaveId er allerede ferdigstilt, callId: $callId" }
                Unit.right()
            } else {
                ferdigstillOppgave(oppgave, callId).map {
                    logger.info { "Ferdigstilt oppgave med id $oppgaveId, callId $callId" }
                }
            }
        }
    }

    override suspend fun erFerdigstilt(oppgaveId: OppgaveId): Either<HttpKlientError, Boolean> {
        val callId = UUID.randomUUID()
        logger.info { "Sjekker om oppgave med id $oppgaveId er ferdigstilt, callId $callId" }
        return getOppgave(oppgaveId, callId).map { it.erFerdigstilt() }
    }

    private suspend fun opprettOppgave(
        opprettOppgaveRequest: OpprettOppgaveRequest,
        callId: UUID,
    ): Either<HttpKlientError, OppgaveId> {
        return httpKlient.post<OpprettOppgaveResponse>(oppgaverUri, opprettOppgaveRequest) {
            header("X-Correlation-ID", callId.toString())
            successStatus(201)
        }.map { response ->
            val oppgaveId = OppgaveId(response.body.id.toString())
            logger.info { "Opprettet oppgave med id $oppgaveId for callId: $callId ${opprettOppgaveRequest.journalpostId?.let { ", journalpostId: $it" }}" }
            oppgaveId
        }
    }

    private suspend fun finnOppgave(
        journalpostId: JournalpostId,
        oppgaveType: String,
        callId: UUID,
    ): Either<HttpKlientError, FinnOppgaveResponse> {
        return httpKlient.get<FinnOppgaveResponse>(finnOppgaveUri(journalpostId, oppgaveType)) {
            header("X-Correlation-ID", callId.toString())
        }.map { it.body }
    }

    private suspend fun getOppgave(
        oppgaveId: OppgaveId,
        callId: UUID,
    ): Either<HttpKlientError, Oppgave> {
        return httpKlient.get<Oppgave>(URI.create("$oppgaverUri/$oppgaveId")) {
            header("X-Correlation-ID", callId.toString())
        }.map { it.body }
    }

    private suspend fun ferdigstillOppgave(
        oppgave: Oppgave,
        callId: UUID,
    ): Either<HttpKlientError, Unit> {
        val ferdigstillOppgaveRequest = FerdigstillOppgaveRequest(
            versjon = oppgave.versjon,
            status = OppgaveStatus.FERDIGSTILT,
        )
        return httpKlient.patch<Unit>(URI.create("$oppgaverUri/${oppgave.id}"), ferdigstillOppgaveRequest) {
            header("X-Correlation-ID", callId.toString())
        }.map { }
    }

    private fun finnOppgaveUri(journalpostId: JournalpostId, oppgaveType: String): URI {
        return URI.create("$oppgaverUri?tema=$TEMA_TILTAKSPENGER&oppgavetype=$oppgaveType&journalpostId=$journalpostId&statuskategori=AAPEN")
    }
}
