package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlientFake
import no.nav.tiltakspenger.libs.httpklient.HttpMethod
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import org.junit.jupiter.api.Test
import java.time.Instant

internal class OppgaveHttpClientTest {
    private val baseUrl = "http://oppgave.test"
    private val fnr = Fnr.random()
    private val journalpostId = JournalpostId("453812134")

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(httpKlient: HttpKlientFake) = OppgaveHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        httpKlient = httpKlient,
    )

    private fun HttpKlientFake.enqueueFinnOppgaveResponse(vararg oppgaver: Oppgave) {
        enqueueResponse(
            body = FinnOppgaveResponse(
                antallTreffTotalt = oppgaver.size,
                oppgaver = oppgaver.toList(),
            ),
        )
    }

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        OppgaveHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `opprettOppgave - ingen eksisterende oppgave - søker og oppretter`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueFinnOppgaveResponse()
            enqueueResponse(body = OpprettOppgaveResponse(id = 42), statusCode = 201)
        }

        runTest {
            client(httpKlient).opprettOppgave(fnr, journalpostId, Oppgavebehov.NYTT_MELDEKORT) shouldBe OppgaveId("42").right()
        }

        val (finnRequest, opprettRequest) = httpKlient.requests
        finnRequest.method shouldBe HttpMethod.GET
        finnRequest.uri.toString() shouldBe
            "$baseUrl/api/v1/oppgaver?tema=IND&oppgavetype=VURD_HENV&journalpostId=$journalpostId&statuskategori=AAPEN"
        finnRequest.headers["X-Correlation-ID"]?.single().shouldNotBeNull()
        opprettRequest.method shouldBe HttpMethod.POST
        opprettRequest.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver"
    }

    @Test
    fun `opprettOppgave - oppgave finnes fra før - returnerer eksisterende uten å opprette`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueFinnOppgaveResponse(Oppgave(id = 123, status = OppgaveStatus.OPPRETTET, versjon = 1))
        }

        runTest {
            client(httpKlient).opprettOppgave(fnr, journalpostId, Oppgavebehov.NY_SOKNAD) shouldBe OppgaveId("123").right()
        }

        httpKlient.requests.size shouldBe 1
    }

    @Test
    fun `opprettOppgave - søket feiler - gir Left uten å opprette`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "feil") }

        runTest {
            client(httpKlient).opprettOppgave(fnr, journalpostId, Oppgavebehov.NY_SOKNAD).isLeft() shouldBe true
        }

        httpKlient.requests.size shouldBe 1
    }

    @Test
    fun `opprettOppgave - ukjent oppgavebehov er en programmeringsfeil og kaster`() {
        runTest {
            shouldThrow<IllegalArgumentException> {
                client(HttpKlientFake()).opprettOppgave(fnr, journalpostId, Oppgavebehov.DOED)
            }
        }
    }

    @Test
    fun `opprettOppgaveUtenDuplikatkontroll - oppretter for alle støttede oppgavebehov`() {
        val støttedeBehov = listOf(
            Oppgavebehov.ENDRET_TILTAKDELTAKER,
            Oppgavebehov.FATT_BARN,
            Oppgavebehov.DOED,
            Oppgavebehov.ADRESSEBESKYTTELSE,
        )
        val httpKlient = HttpKlientFake().apply {
            repeat(støttedeBehov.size) { enqueueResponse(body = OpprettOppgaveResponse(id = 42), statusCode = 201) }
        }

        runTest {
            støttedeBehov.forEach { behov ->
                client(httpKlient).opprettOppgaveUtenDuplikatkontroll(
                    fnr = fnr,
                    oppgavebehov = behov,
                    tilleggstekst = "endring i deltakelse",
                ) shouldBe OppgaveId("42").right()
            }
        }

        httpKlient.requests.size shouldBe støttedeBehov.size
        httpKlient.requests.forEach { it.method shouldBe HttpMethod.POST }
    }

    @Test
    fun `opprettOppgaveUtenDuplikatkontroll - oppgavebehov med duplikatkontroll er en programmeringsfeil og kaster`() {
        runTest {
            shouldThrow<IllegalArgumentException> {
                client(HttpKlientFake()).opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.NYTT_MELDEKORT)
            }
        }
    }

    @Test
    fun `ferdigstillOppgave - åpen oppgave - henter og ferdigstiller`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueResponse(body = Oppgave(id = 50, status = OppgaveStatus.UNDER_BEHANDLING, versjon = 3))
            enqueueUnitResponse(statusCode = 200)
        }

        runTest {
            client(httpKlient).ferdigstillOppgave(OppgaveId("50")) shouldBe Unit.right()
        }

        val (getRequest, patchRequest) = httpKlient.requests
        getRequest.method shouldBe HttpMethod.GET
        getRequest.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver/50"
        patchRequest.method shouldBe HttpMethod.PATCH
        patchRequest.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver/50"
    }

    @Test
    fun `ferdigstillOppgave - allerede ferdigstilt - gjør ikke noe`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueResponse(body = Oppgave(id = 50, status = OppgaveStatus.FERDIGSTILT, versjon = 3))
        }

        runTest {
            client(httpKlient).ferdigstillOppgave(OppgaveId("50")) shouldBe Unit.right()
        }

        httpKlient.requests.size shouldBe 1
    }

    @Test
    fun `ferdigstillOppgave - henting av oppgaven feiler - gir Left`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "feil") }

        runTest {
            client(httpKlient).ferdigstillOppgave(OppgaveId("50")).isLeft() shouldBe true
        }
    }

    @Test
    fun `erFerdigstilt - feilregistrert regnes som ferdigstilt`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueResponse(body = Oppgave(id = 50, status = OppgaveStatus.FEILREGISTRERT, versjon = 3))
        }

        runTest {
            client(httpKlient).erFerdigstilt(OppgaveId("50")) shouldBe true.right()
        }
    }

    @Test
    fun `erFerdigstilt - åpen oppgave gir false`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueResponse(body = Oppgave(id = 50, status = OppgaveStatus.AAPNET, versjon = 3))
        }

        runTest {
            client(httpKlient).erFerdigstilt(OppgaveId("50")) shouldBe false.right()
        }
    }
}
