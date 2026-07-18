package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.journalpost.HentDokumentCommand
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tester klienten mot `FakeHttpTransport` slik at hele den reelle `HttpKlient`-pipelinen kjører (statusregel, retry, auth, binær dekoding).
 */
internal class SafJournalpostHttpClientTest {

    private val texasClient = mockk<TexasClient>()
    private val systemTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("system-token", Instant.parse("2026-01-01T00:00:00Z"))
    }
    private val journalpostId = JournalpostId("467010363")
    private val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0xFF.toByte(), 0xFE.toByte())

    private fun nyKlient(transport: FakeHttpTransport) = SafJournalpostHttpClient(
        baseUrl = "http://saf",
        safScope = "saf-scope",
        texasClient = texasClient,
        authTokenProvider = systemTokenProvider,
        clock = fixedClock,
        transport = transport,
    )

    private fun mockOboVeksling() {
        coEvery {
            texasClient.exchangeToken(
                userToken = "saksbehandler-token",
                audienceTarget = "saf-scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } returns AccessToken("obo-token", Instant.parse("2026-01-01T00:00:00Z"))
    }

    private fun hentDokumentCommand() = HentDokumentCommand(
        sakId = SakId.random(),
        journalpostId = journalpostId,
        dokumentInfoId = DokumentInfoId("123"),
        saksbehandlerToken = "saksbehandler-token",
        saksbehandler = ObjectMother.saksbehandler(),
        correlationId = CorrelationId.generate(),
    )

    private fun graphQLJournalpostJson(datoOpprettet: String? = "2025-01-01T01:02:03") = """
        {"data":{"journalpost":{"avsenderMottaker":{"id":"12345678911","type":"FNR"},"datoOpprettet":${datoOpprettet?.let { "\"$it\"" }},"bruker":{"id":"12345678911","type":"FNR"}}}}
    """.trimIndent()

    @Test
    fun `bygger default transport når transport ikke sendes inn`() {
        SafJournalpostHttpClient(
            baseUrl = "http://saf",
            safScope = "saf-scope",
            texasClient = texasClient,
            authTokenProvider = systemTokenProvider,
            clock = fixedClock,
        )
    }

    @Test
    fun `hentJournalpost - 200 med data gir journalpost og POSTer GraphQL med systemtoken`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(graphQLJournalpostJson()) }

        val journalpost = nyKlient(transport).hentJournalpost(journalpostId).getOrFail().shouldNotBeNull()

        journalpost.bruker?.id shouldBe "12345678911"
        journalpost.datoOpprettet shouldBe "2025-01-01T01:02:03"
        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "http://saf/graphql"
        kall.request.headers().firstValue("Authorization").get() shouldBe "Bearer system-token"
        kall.request.headers().firstValue("X-Correlation-ID").get() shouldBe journalpostId.toString()
        kall.bodyTekst.contains("FindJournalpost") shouldBe true
    }

    @Test
    fun `hentJournalpost - not_found og bad_request i errors-lista betyr at journalposten ikke finnes`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("""{"data":null,"errors":[{"message":"ikke funnet","extensions":{"code":"not_found","classification":"ExecutionAborted"}}]}""")
            leggIKøJson("""{"data":null,"errors":[{"message":"ugyldig","extensions":{"code":"bad_request","classification":"ValidationError"}}]}""")
        }
        val klient = nyKlient(transport)

        klient.hentJournalpost(journalpostId).getOrFail().shouldBeNull()
        klient.hentJournalpost(journalpostId).getOrFail().shouldBeNull()
    }

    @Test
    fun `hentJournalpost - andre GraphQL-feilkoder gir GraphQLFeil`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("""{"data":null,"errors":[{"message":"nope","extensions":{"code":"forbidden","classification":"ExecutionAborted"}},{"message":"uten extensions"}]}""")
        }

        val feil = nyKlient(transport).hentJournalpost(journalpostId).swap().getOrNull().shouldNotBeNull()

        val graphQLFeil = feil.shouldBeInstanceOf<KanIkkeHenteJournalpost.GraphQLFeil>()
        graphQLFeil.feilkoder shouldBe listOf("forbidden", "ukjent")
        // rawResponseString ligger i metadata slik at kalleren kan sende hele feilresponsen til sikkerlogg.
        graphQLFeil.httpKlientMetadata.rawResponseString.shouldNotBeNull().contains("nope") shouldBe true
        feil.beskrivelse() shouldBe "GraphQLFeil(feilkoder=[forbidden, ukjent])"
    }

    @Test
    fun `hentJournalpost - blanding av not_found og reell feil gir GraphQLFeil`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("""{"data":null,"errors":[{"message":"ikke funnet","extensions":{"code":"not_found"}},{"message":"boom","extensions":{"code":"server_error"}}]}""")
        }

        val feil = nyKlient(transport).hentJournalpost(journalpostId).swap().getOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<KanIkkeHenteJournalpost.GraphQLFeil>().feilkoder shouldBe listOf("not_found", "server_error")
    }

    @Test
    fun `hentJournalpost - manglende journalpost-data uten errors betyr at journalposten ikke finnes`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson("""{"data":{"journalpost":null}}""") }

        nyKlient(transport).hentJournalpost(journalpostId).getOrFail().shouldBeNull()
    }

    @Test
    fun `hentJournalpost - journalpost uten datoOpprettet behandles som at den ikke finnes`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(graphQLJournalpostJson(datoOpprettet = null)) }

        nyKlient(transport).hentJournalpost(journalpostId).getOrFail().shouldBeNull()
    }

    @Test
    fun `hentJournalpost - ikke-retrybar feilstatus gir KallFeilet`() = runTest {
        // 403 er ikke retrybar, så ett køet svar holder selv om klienten har retry.
        val transport = FakeHttpTransport().apply { leggIKøStatus(403, body = "forbidden") }

        val feil = nyKlient(transport).hentJournalpost(journalpostId).swap().getOrNull().shouldNotBeNull()

        val kallFeilet = feil.shouldBeInstanceOf<KanIkkeHenteJournalpost.KallFeilet>()
        kallFeilet.httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 403
        feil.beskrivelse() shouldBe "KallFeilet(UventetStatus)"
    }

    @Test
    fun `hentJournalpost - 500 retryes selv om GraphQL-oppslaget går som POST`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(500, body = "boom")
            leggIKøJson(graphQLJournalpostJson())
        }

        nyKlient(transport).hentJournalpost(journalpostId).getOrFail().shouldNotBeNull()

        transport.mottatteKall.size shouldBe 2
    }

    @Test
    fun `hentDokument - 200 gir PdfA med bytene eksakt og OBO-token på kallet`() = runTest {
        mockOboVeksling()
        val transport = FakeHttpTransport().apply { leggIKøBytes(pdfBytes, contentType = "application/pdf") }
        val command = hentDokumentCommand()

        val pdf = nyKlient(transport).hentDokument(command).getOrFail()

        pdf.getContent().toList() shouldBe pdfBytes.toList()
        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "GET"
        kall.uri.toString() shouldBe "http://saf/rest/hentdokument/$journalpostId/123/ARKIV"
        kall.request.headers().firstValue("Authorization").get() shouldBe "Bearer obo-token"
        kall.request.headers().firstValue("X-Correlation-ID").get() shouldBe command.correlationId.value
        kall.request.headers().firstValue("Nav-Callid").get() shouldBe command.correlationId.value
    }

    @Test
    fun `hentDokument - feilstatus gir Left`() = runTest {
        mockOboVeksling()
        val transport = FakeHttpTransport().apply { leggIKøStatus(404, body = "finnes ikke", contentType = "text/plain") }

        val feil = nyKlient(transport).hentDokument(hentDokumentCommand()).swap().getOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 404
    }

    @Test
    fun `hentDokument - feilet OBO-veksling gir AuthError uten at noe kall gjøres`() = runTest {
        coEvery {
            texasClient.exchangeToken(
                userToken = "saksbehandler-token",
                audienceTarget = "saf-scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } throws IllegalStateException("texas er nede")
        val transport = FakeHttpTransport()

        val feil = nyKlient(transport).hentDokument(hentDokumentCommand()).swap().getOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<HttpKlientError.AuthError>()
        transport.mottatteKall shouldBe emptyList()
    }
}
