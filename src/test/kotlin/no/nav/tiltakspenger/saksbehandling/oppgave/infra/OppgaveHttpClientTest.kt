package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import arrow.core.right
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveHttpClientTest {
    private val baseUrl = "http://oppgave.test"
    private val fnr = Fnr.random()
    private val journalpostId = JournalpostId("453812134")

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport = FakeHttpTransport(), clock: Clock = ObjectMother.clock) = OppgaveHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = clock,
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
        finnKall.request.headers().firstValue("X-Correlation-ID").orElse(null).shouldNotBeNull()
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
    fun `opprettOppgave - alle oppgavebehov uten journalpost er en programmeringsfeil og kaster`() {
        val behovUtenJournalpost = listOf(
            Oppgavebehov.FATT_BARN,
            Oppgavebehov.DOED,
            Oppgavebehov.ADRESSEBESKYTTELSE,
        )

        runTest {
            behovUtenJournalpost.forEach { behov ->
                shouldThrow<IllegalArgumentException> {
                    client().opprettOppgave(fnr, journalpostId, behov)
                }
            }
        }
    }

    /** Oppgave-APIet kan svare med et treff-antall uten å levere oppgavene — da oppretter vi heller enn å anta en id vi ikke har. */
    @Test
    fun `opprettOppgave - treff uten oppgaveliste - oppretter ny oppgave`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(FinnOppgaveResponse(antallTreffTotalt = 1, oppgaver = emptyList()))
            leggIKøJson(OpprettOppgaveResponse(id = 42), statusCode = 201)
        }

        runTest {
            client(transport).opprettOppgave(fnr, journalpostId, Oppgavebehov.NY_SOKNAD) shouldBe OppgaveId("42").right()
        }

        transport.mottatteKall.size shouldBe 2
    }

    @Test
    fun `opprettOppgaveUtenDuplikatkontroll - oppretter for alle støttede oppgavebehov`() {
        val støttedeBehov = listOf(
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
                ) shouldBe OppgaveId("42").right()
            }
        }

        transport.mottatteKall.size shouldBe støttedeBehov.size
        transport.mottatteKall.forEach { it.metode shouldBe "POST" }
    }

    @Test
    fun `opprettOppgaveUtenDuplikatkontroll - alle oppgavebehov med duplikatkontroll er en programmeringsfeil og kaster`() {
        runTest {
            listOf(Oppgavebehov.NYTT_MELDEKORT, Oppgavebehov.NY_SOKNAD).forEach { behov ->
                shouldThrow<IllegalArgumentException> {
                    client().opprettOppgaveUtenDuplikatkontroll(fnr, behov)
                }
            }
        }
    }

    /*
     * Karakteriseringstester: de pinner hva vi sender til oppgavetjenesten (backend for Gosys) i dag, slik at endringer for #94 ikke endrer dette ved et uhell.
     *
     * Felt i OpprettOppgaveRequest som ikke forklarer seg selv:
     * - opprettetAvEnhetsnr "9999": betyr «opprettet automatisk av et system», ikke av et Nav-kontor.
     * - journalpostId: kobler oppgaven til søknaden eller meldekortet i arkivet.
     *   Bare NY_SOKNAD og NYTT_MELDEKORT har journalpost, de andre sender null.
     * - tema "IND": temaet «Individstønad», som tiltakspenger hører til.
     * - oppgavetype: BEH_SAK = «Behandle sak», VURD_HENV = «Vurder henvendelse», VUR_KONS_YTE = «Vurder konsekvens for ytelse».
     * - fristFerdigstillelse: i dag + 3 dager, flyttet til mandag hvis den havner i en helg.
     *   Med klokka 2025-05-01 (torsdag) blir fristen søndag 4. mai, som flyttes til mandag 5. mai.
     *
     * Vi setter ikke tildeltEnhetsnr eller mappeId.
     * Da finner oppgavetjenesten selv riktig enhet ut fra personen, og det er derfor kode 6/7 og skjermede havner hos spesialenhetene.
     */

    @ParameterizedTest
    @CsvSource(
        "2025-05-01, 2025-05-05",
        "2025-05-05, 2025-05-08",
    )
    fun `opprettOppgave - ny søknad - søker etter åpen BEH_SAK på journalposten og sender samme body som før`(
        aktivDato: LocalDate,
        forventetFrist: LocalDate,
    ) {
        val transport = FakeHttpTransport().apply {
            // Første svar: duplikatsøket finner ingen åpne oppgaver.
            leggIKøFinnOppgaveResponse()
            // Andre svar: oppgavetjenesten har opprettet oppgaven.
            leggIKøJson(OpprettOppgaveResponse(id = 42), statusCode = 201)
        }
        val clock = fixedClockAt(aktivDato.atTime(12, 0))

        runTest {
            client(transport, clock).opprettOppgave(fnr, journalpostId, Oppgavebehov.NY_SOKNAD) shouldBe OppgaveId("42").right()
        }

        transport.mottatteKall.also { (finnKall, opprettKall) ->
            // Duplikatsøket: finnes det allerede en åpen BEH_SAK-oppgave på denne journalposten?
            // statuskategori=AAPEN betyr at bare oppgaver som ikke er ferdigstilt eller feilregistrert regnes som duplikat.
            finnKall.metode shouldBe "GET"
            finnKall.uri.toString() shouldBe
                "$baseUrl/api/v1/oppgaver?tema=IND&oppgavetype=BEH_SAK&journalpostId=$journalpostId&statuskategori=AAPEN"

            opprettKall.metode shouldBe "POST"
            opprettKall.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver"
            opprettKall.bodyTekst.shouldNotBeNull() shouldEqualJson """
                {
                  "personident": "${fnr.verdi}",
                  "opprettetAvEnhetsnr": "9999",
                  "journalpostId": "$journalpostId",
                  "behandlesAvApplikasjon": null,
                  "beskrivelse": "Ny søknad om tiltakspenger. Behandles i ny løsning.",
                  "tema": "IND",
                  "oppgavetype": "BEH_SAK",
                  "aktivDato": "$aktivDato",
                  "fristFerdigstillelse": "$forventetFrist",
                  "prioritet": "NORM"
                }
            """.trimIndent()
        }
    }

    @Test
    fun `opprettOppgave - nytt meldekort - sender samme body som før`() {
        val transport = FakeHttpTransport().apply {
            leggIKøFinnOppgaveResponse()
            leggIKøJson(OpprettOppgaveResponse(id = 42), statusCode = 201)
        }
        val clock = fixedClockAt(LocalDateTime.of(2025, 5, 1, 12, 0))

        runTest {
            client(transport, clock).opprettOppgave(fnr, journalpostId, Oppgavebehov.NYTT_MELDEKORT) shouldBe OppgaveId("42").right()
        }

        // Duplikatsøket for meldekort er allerede pinnet i `opprettOppgave - ingen eksisterende oppgave - søker og oppretter`.
        // Her holder det å pinne opprettelsen.
        transport.mottatteKall.also { (_, opprettKall) ->
            opprettKall.metode shouldBe "POST"
            opprettKall.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver"
            // Meldekort er en «henvendelse» i Gosys-språk, ikke en sak, derfor VURD_HENV og ikke BEH_SAK som for søknaden.
            opprettKall.bodyTekst.shouldNotBeNull() shouldEqualJson """
                {
                  "personident": "${fnr.verdi}",
                  "opprettetAvEnhetsnr": "9999",
                  "journalpostId": "$journalpostId",
                  "behandlesAvApplikasjon": null,
                  "beskrivelse": "Nytt meldekort for tiltakspenger. Behandles i ny løsning.",
                  "tema": "IND",
                  "oppgavetype": "VURD_HENV",
                  "aktivDato": "2025-05-01",
                  "fristFerdigstillelse": "2025-05-05",
                  "prioritet": "NORM"
                }
            """.trimIndent()
        }
    }

    @ParameterizedTest
    @CsvSource(
        "FATT_BARN, VUR_KONS_YTE, 'Mottaker av tiltakspenger har fått barn. Dette kan påvirke tiltakspengeytelsen.'",
        "DOED, VUR_KONS_YTE, 'Mottaker av tiltakspenger er registrert som død.'",
        "ADRESSEBESKYTTELSE, BEH_SAK, 'Mottaker av tiltakspenger har fått strengt fortrolig adresse og har en åpen tiltakspengesak som må følges opp i ny løsning.'",
    )
    fun `opprettOppgaveUtenDuplikatkontroll - personhendelse - sender samme body som før`(
        behov: Oppgavebehov,
        oppgavetype: String,
        beskrivelse: String,
    ) {
        // Personhendelser (fra PDL via PersonhendelseJobb) har ingen journalpost, og vi sjekker ikke duplikater mot oppgavetjenesten.
        // Duplikater unngås i stedet ved at oppgaveId lagres på personhendelsen i vår database.
        val transport = FakeHttpTransport().apply { leggIKøJson(OpprettOppgaveResponse(id = 42), statusCode = 201) }
        val clock = fixedClockAt(LocalDateTime.of(2025, 5, 1, 12, 0))

        runTest {
            client(transport, clock).opprettOppgaveUtenDuplikatkontroll(fnr, behov) shouldBe OppgaveId("42").right()
        }

        // Oppgavetype og beskrivelse varierer med behov og kommer inn som parametere fra CsvSource.
        transport.mottatteKall.single().also { kall ->
            kall.metode shouldBe "POST"
            kall.uri.toString() shouldBe "$baseUrl/api/v1/oppgaver"
            kall.bodyTekst.shouldNotBeNull() shouldEqualJson """
                {
                  "personident": "${fnr.verdi}",
                  "opprettetAvEnhetsnr": "9999",
                  "journalpostId": null,
                  "behandlesAvApplikasjon": null,
                  "beskrivelse": ${serialize(beskrivelse)},
                  "tema": "IND",
                  "oppgavetype": "$oppgavetype",
                  "aktivDato": "2025-05-01",
                  "fristFerdigstillelse": "2025-05-05",
                  "prioritet": "NORM"
                }
            """.trimIndent()
        }
    }

    @Test
    fun `opprettOppgaveUtenDuplikatkontroll - feil ved opprettelse returnerer Left`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil") }

        runTest {
            client(transport).opprettOppgaveUtenDuplikatkontroll(fnr, Oppgavebehov.FATT_BARN).isLeft() shouldBe true
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
