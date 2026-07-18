package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.KunneIkkeJournalføre
import no.nav.tiltakspenger.saksbehandling.journalføring.beskrivelse
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tester klienten mot `FakeHttpTransport` slik at hele den reelle `HttpKlient`-pipelinen kjører (statusregel, retry, auth, deserialisering).
 * [Klagevedtak] mockes fordi et gyldig klagevedtak krever hele avvisnings-flyten; journalpost-mapperen leser kun fnr og saksnummer.
 */
internal class DokarkivHttpClientTest {

    private val systemTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("system-token", Instant.parse("2026-01-01T00:00:00Z"))
    }
    private val correlationId = CorrelationId.generate()
    private val pdfOgJson = PdfOgJson(PdfA("pdf".toByteArray()), """{"brev":"json"}""")

    private fun nyKlient(transport: FakeHttpTransport) = DokarkivHttpClient(
        baseUrl = "http://dokarkiv",
        clock = fixedClock,
        authTokenProvider = systemTokenProvider,
        transport = transport,
    )

    private fun dokarkivOkJson(
        journalpostId: String? = "467010363",
        journalpostferdigstilt: Boolean? = true,
        dokumenter: String = """[{"dokumentInfoId":"123"},{"dokumentInfoId":"456"}]""",
    ) = """{"journalpostId":${journalpostId?.let { "\"$it\"" }},"journalpostferdigstilt":$journalpostferdigstilt,"melding":null,"dokumenter":$dokumenter}"""

    @Test
    fun `bygger default transport når transport ikke sendes inn`() {
        DokarkivHttpClient(
            baseUrl = "http://dokarkiv",
            clock = fixedClock,
            authTokenProvider = systemTokenProvider,
        )
    }

    @Test
    fun `201 gir journalførte dokumenter og POSTer med forsoekFerdigstill og systemtoken`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(), statusCode = 201) }
        val vedtak = ObjectMother.nyRammevedtakInnvilgelse()

        val dokumenter = nyKlient(transport).journalførVedtaksbrevForRammevedtak(vedtak, pdfOgJson, correlationId).getOrFail()

        dokumenter.journalpostId shouldBe JournalpostId("467010363")
        dokumenter.dokumentInfoIder shouldBe nonEmptyListOf(DokumentInfoId("123"), DokumentInfoId("456"))
        dokumenter.metadata.responseStatus shouldBe "201 Created"
        dokumenter.metadata.requestBody shouldContain "fysiskDokument"
        dokumenter.metadata.responseBody shouldContain "467010363"
        dokumenter.metadata.journalføringsTidspunkt shouldBe nå(fixedClock)

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "http://dokarkiv/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
        kall.request.headers().firstValue("Authorization").get() shouldBe "Bearer system-token"
        kall.request.headers().firstValue("X-Correlation-ID").get() shouldBe correlationId.value
        kall.request.headers().firstValue("Nav-Callid").get() shouldBe correlationId.value
        kall.request.headers().firstValue("Content-Type").get() shouldBe "application/json"
        kall.bodyTekst shouldBe dokumenter.metadata.requestBody
    }

    @Test
    fun `201 uten ferdigstilt journalpost er fortsatt suksess`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(journalpostferdigstilt = false), statusCode = 201) }

        val dokumenter = nyKlient(transport)
            .journalførVedtaksbrevForMeldekortvedtak(ObjectMother.meldekortvedtak(opprettet = nå(fixedClock)), pdfOgJson, correlationId)
            .getOrFail()

        dokumenter.journalpostId shouldBe JournalpostId("467010363")
    }

    @Test
    fun `201 uten journalpostId gir UgyldigRespons`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(journalpostId = null), statusCode = 201) }

        val feil = nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(ObjectMother.nyRammevedtakInnvilgelse(), pdfOgJson, correlationId)
            .swap()
            .getOrNull()
            .shouldNotBeNull()

        val ugyldig = feil.shouldBeInstanceOf<KunneIkkeJournalføre.UgyldigRespons>()
        ugyldig.metadata.rawResponseString.shouldNotBeNull() shouldContain "journalpostferdigstilt"
        feil.beskrivelse() shouldContain "UgyldigRespons"
    }

    @Test
    fun `201 uten dokumenter gir null dokumentInfoIder`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(dokumenter = "[]"), statusCode = 201) }

        val dokumenter = nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(ObjectMother.nyRammevedtakInnvilgelse(), pdfOgJson, correlationId)
            .getOrFail()

        dokumenter.dokumentInfoIder.shouldBeNull()
    }

    @Test
    fun `409 betyr allerede journalført og er suksess`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(), statusCode = 409) }

        val dokumenter = nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(ObjectMother.nyRammevedtakInnvilgelse(), pdfOgJson, correlationId)
            .getOrFail()

        dokumenter.journalpostId shouldBe JournalpostId("467010363")
        dokumenter.metadata.responseStatus shouldBe "409 Conflict"
        // Retry skal ikke slå inn på 409 - den er et domeneutfall, ikke en transient feil.
        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `409 uten journalpostId gir UgyldigRespons`() = runTest {
        // Gammel klient godtok tom journalpostId på 409 og persisterte JournalpostId("") - det skal vi ikke lenger.
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(journalpostId = ""), statusCode = 409) }

        val feil = nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(ObjectMother.nyRammevedtakInnvilgelse(), pdfOgJson, correlationId)
            .swap()
            .getOrNull()
            .shouldNotBeNull()

        feil.shouldBeInstanceOf<KunneIkkeJournalføre.UgyldigRespons>()
    }

    @Test
    fun `409 med udeserialiserbar body gir KallFeilet`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatus(409, body = "<html>ikke json</html>", contentType = "text/html") }

        val feil = nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(ObjectMother.nyRammevedtakInnvilgelse(), pdfOgJson, correlationId)
            .swap()
            .getOrNull()
            .shouldNotBeNull()

        feil.shouldBeInstanceOf<KunneIkkeJournalføre.KallFeilet>().feil.shouldBeInstanceOf<HttpKlientError.DeserializationError>()
    }

    @Test
    fun `400 gir KallFeilet uten retry`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatus(400, body = """{"melding":"ugyldig"}""") }

        val feil = nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(ObjectMother.nyRammevedtakInnvilgelse(), pdfOgJson, correlationId)
            .swap()
            .getOrNull()
            .shouldNotBeNull()

        val kallFeilet = feil.shouldBeInstanceOf<KunneIkkeJournalføre.KallFeilet>()
        kallFeilet.feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 400
        kallFeilet.beskrivelse() shouldBe "KallFeilet(UventetStatus)"
        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `500 retryes selv om journalføring går som POST`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(500, body = "boom")
            leggIKøJson(dokarkivOkJson(), statusCode = 201)
        }

        nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(ObjectMother.nyRammevedtakInnvilgelse(), pdfOgJson, correlationId)
            .getOrFail()

        transport.mottatteKall.size shouldBe 2
    }

    @Test
    fun `request-bygging som kaster gir KunneIkkeByggeRequest uten at noe kall gjøres`() = runTest {
        val transport = FakeHttpTransport()
        val vedtak = mockk<Rammevedtak> {
            every { fnr } throws IllegalStateException("mangler fnr")
        }

        val feil = nyKlient(transport)
            .journalførVedtaksbrevForRammevedtak(vedtak, pdfOgJson, correlationId)
            .swap()
            .getOrNull()
            .shouldNotBeNull()

        feil.shouldBeInstanceOf<KunneIkkeJournalføre.KunneIkkeByggeRequest>().throwable.shouldBeInstanceOf<IllegalStateException>()
        feil.beskrivelse() shouldBe "KunneIkkeByggeRequest(IllegalStateException)"
        transport.mottatteKall shouldBe emptyList()
    }

    @Test
    fun `tilResponseStatusTekst mapper kun 201 og 409 og feiler for andre statuser`() {
        201.tilResponseStatusTekst() shouldBe "201 Created"
        409.tilResponseStatusTekst() shouldBe "409 Conflict"
        shouldThrow<IllegalStateException> { 500.tilResponseStatusTekst() }
    }

    @Test
    fun `journalfører avvisningsvedtak for klagevedtak`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(), statusCode = 201) }
        val klagevedtak = mockk<Klagevedtak> {
            every { fnr } returns Fnr.random()
            every { saksnummer } returns Saksnummer.genererSaknummer(3.desember(2025), "4050")
            every { id } returns VedtakId.random()
        }

        val dokumenter = nyKlient(transport).journalførAvvisningsvedtakForKlagevedtak(klagevedtak, pdfOgJson, correlationId).getOrFail()

        dokumenter.journalpostId shouldBe JournalpostId("467010363")
        transport.mottatteKall.single().bodyTekst shouldContain "KLAGE-AVVISNING-TILTAKSPENGER"
    }

    @Test
    fun `journalfører innstillingsbrev for opprettholdt klagebehandling`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(dokarkivOkJson(), statusCode = 201) }
        val klagebehandling = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()

        val dokumenter = nyKlient(transport).journalførInnstillingsbrevForOpprettholdtKlagebehandling(klagebehandling, pdfOgJson, correlationId).getOrFail()

        dokumenter.journalpostId shouldBe JournalpostId("467010363")
        transport.mottatteKall.single().bodyTekst shouldContain "KLAGE-OPPRETTHOLDELSE-TILTAKSPENGER"
    }
}
