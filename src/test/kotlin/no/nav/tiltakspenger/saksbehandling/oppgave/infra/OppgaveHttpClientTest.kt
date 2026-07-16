package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
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

    private fun client(transport: FakeHttpTransport = FakeHttpTransport()) = OppgaveHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        transport = transport,
    )

    private fun FakeHttpTransport.leggIKøFinnOppgaveResponse(vararg oppgaver: Oppgave) {
        leggIKøJson(
            FinnOppgaveResponse(
                antallTreffTotalt = oppgaver.size,
                oppgaver = oppgaver.toList(),
            ),
        )
    }

    @Test
    fun `bygger produksjonstransport når transport ikke sendes inn`() {
        OppgaveHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `opprettOppgave - ingen eksisterende oppgave - søker og oppretter`() {
        val transport = FakeHttpTransport().apply {
            leggIKøFinnOppgaveResponse()
            leggIKøJson(OpprettOppgaveResponse(id = 42), statusCode = 201)
        }

        runTest {
            client(transport).opprettOppgave(fnr, journalpostId, Oppgavebehov.NYTT_MELDEKORT) shouldBe OppgaveId("42").right()
        }

        val (finnKall, opprettKall) = transport.mottatteKall
        finnKall.metode shouldBe "GET"
        finnKall.uri.toString() shouldBe
            "$baseUrl/api/v1/oppgaver?tema=IND&oppgavetype=VURD_HENV&journalpostId=$journalpostId&statuskategori=AAPEN"
        finnKall.request.headers().firstValue("X-Correlation-ID").get().shouldNotBeNull()
        opprettKall.metode shouldBe "POST"
        opprettKall.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver"
    }

    @Test
    fun `opprettOppgave - oppgave finnes fra før - returnerer eksisterende uten å opprette`() {
        val transport = FakeHttpTransport().apply {
            leggIKøFinnOppgaveResponse(Oppgave(id = 123, status = OppgaveStatus.OPPRETTET, versjon = 1))
        }

        runTest {
            client(transport).opprettOppgave(fnr, journalpostId, Oppgavebehov.NY_SOKNAD) shouldBe OppgaveId("123").right()
        }

        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `opprettOppgave - søket feiler - gir Left uten å opprette`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil") }

        runTest {
            client(transport).opprettOppgave(fnr, journalpostId, Oppgavebehov.NY_SOKNAD).isLeft() shouldBe true
        }

        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `opprettOppgave - ukjent oppgavebehov er en programmeringsfeil og kaster`() {
        runTest {
            shouldThrow<IllegalArgumentException> {
                client().opprettOppgave(fnr, journalpostId, Oppgavebehov.DOED)
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
        val transport = FakeHttpTransport().apply {
            repeat(støttedeBehov.size) { leggIKøJson(OpprettOppgaveResponse(id = 42), statusCode = 201) }
        }

        runTest {
            støttedeBehov.forEach { behov ->
                client(transport).opprettOppgaveUtenDuplikatkontroll(
                    fnr = fnr,
                    oppgavebehov = behov,
                    tilleggstekst = "endring i deltakelse",
                ) shouldBe OppgaveId("42").right()
            }
        }

        transport.mottatteKall.size shouldBe støttedeBehov.size
        transport.mottatteKall.forEach { it.metode shouldBe "POST" }
    }

    @Test
    fun `opprettOppgaveUtenDuplikatkontroll - oppgavebehov med duplikatkontroll er en programmeringsfeil og kaster`() {
        runTest {
            shouldThrow<IllegalArgumentException> {
                client().opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.NYTT_MELDEKORT)
            }
        }
    }

    @Test
    fun `ferdigstillOppgave - åpen oppgave - henter og ferdigstiller`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(Oppgave(id = 50, status = OppgaveStatus.UNDER_BEHANDLING, versjon = 3))
            leggIKøStatus(statusCode = 200)
        }

        runTest {
            client(transport).ferdigstillOppgave(OppgaveId("50")) shouldBe Unit.right()
        }

        val (getKall, patchKall) = transport.mottatteKall
        getKall.metode shouldBe "GET"
        getKall.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver/50"
        patchKall.metode shouldBe "PATCH"
        patchKall.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver/50"
    }

    @Test
    fun `ferdigstillOppgave - allerede ferdigstilt - gjør ikke noe`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(Oppgave(id = 50, status = OppgaveStatus.FERDIGSTILT, versjon = 3))
        }

        runTest {
            client(transport).ferdigstillOppgave(OppgaveId("50")) shouldBe Unit.right()
        }

        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `ferdigstillOppgave - henting av oppgaven feiler - gir Left`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil") }

        runTest {
            client(transport).ferdigstillOppgave(OppgaveId("50")).isLeft() shouldBe true
        }
    }

    @Test
    fun `erFerdigstilt - feilregistrert regnes som ferdigstilt`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(Oppgave(id = 50, status = OppgaveStatus.FEILREGISTRERT, versjon = 3))
        }

        runTest {
            client(transport).erFerdigstilt(OppgaveId("50")) shouldBe true.right()
        }
    }

    @Test
    fun `erFerdigstilt - åpen oppgave gir false`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(Oppgave(id = 50, status = OppgaveStatus.AAPNET, versjon = 3))
        }

        runTest {
            client(transport).erFerdigstilt(OppgaveId("50")) shouldBe false.right()
        }
    }
}
