package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.pdl.PdlIdentklient
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.TiltakshistorikkHenter
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.TiltakshistorikkKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tester hele den reelle pipelinen (PDL-identoppslag + tiltakshistorikk + mapping) gjennom [FakeHttpTransport].
 * Selve kontrakts- og mappingdetaljene er dekket av testene i `tiltaksdeltakelse-infrastruktur` og [TiltakshistorikkTilRegisterMapperTest].
 */
class TiltakshistorikkHttpKlientTest {
    private val historikkBaseUrl = "http://tiltakshistorikk.test"
    private val pdlBaseUrl = "http://pdl.test"
    private val fnr = Fnr.random()

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(
        pdlTransport: FakeHttpTransport,
        historikkTransport: FakeHttpTransport,
    ) = TiltakshistorikkHttpKlient(
        henteTjeneste = TiltakshistorikkHenter(
            tiltakshistorikkKlient = TiltakshistorikkKlient(
                baseUrl = historikkBaseUrl,
                authTokenProvider = authTokenProvider,
                clock = fixedClock,
                transport = historikkTransport,
            ),
            pdlIdentklient = PdlIdentklient(
                baseUrl = pdlBaseUrl,
                authTokenProvider = authTokenProvider,
                clock = fixedClock,
                transport = pdlTransport,
            ),
            clock = fixedClock,
        ),
        clock = fixedClock,
    )

    private fun pdlJson(vararg identer: Fnr) = """
        {"data": {"hentIdenter": {"identer": [${identer.joinToString(",") { """{"ident": "${it.verdi}"}""" }}]}}, "errors": null}
    """.trimIndent()

    private fun arenaRadJson(
        arenaId: Int = 142536,
        status: String = "GJENNOMFORES",
        tiltakskode: String = "INDOPPFAG",
        startDato: String? = "2024-01-01",
        sluttDato: String? = "2024-06-30",
    ) = """
        {
          "type": "ArenaDeltakelse",
          "norskIdent": "${fnr.verdi}",
          "startDato": ${startDato?.let { "\"$it\"" }},
          "sluttDato": ${sluttDato?.let { "\"$it\"" }},
          "id": "019018e5-6461-74a0-9d66-70d0bf3d0b8b",
          "tittel": "Oppfølging hos Arrangør AS",
          "arenaId": $arenaId,
          "status": "$status",
          "tiltakstype": { "tiltakskode": "$tiltakskode", "navn": "Oppfølging" },
          "gjennomforing": { "id": "0190c9a2-1111-7000-8000-000000000001", "navn": null, "deltidsprosent": 50.0 },
          "arrangor": { "hovedenhet": null, "underenhet": { "organisasjonsnummer": "912345678", "navn": "Arrangør AS" } },
          "deltidsprosent": 100.0,
          "dagerPerUke": 5.0
        }
    """.trimIndent()

    private fun responsJson(rader: List<String>) = """
        {"historikk": [${rader.joinToString(",")}]}
    """.trimIndent()

    private fun transports(vararg rader: String): Pair<FakeHttpTransport, FakeHttpTransport> {
        val pdlTransport = FakeHttpTransport().apply { leggIKøJson(pdlJson(fnr)) }
        val historikkTransport = FakeHttpTransport().apply { leggIKøJson(responsJson(rader.toList())) }
        return pdlTransport to historikkTransport
    }

    @Test
    fun `hentTiltaksdeltakelser - mapper svaret og filtrerer vekk tiltak uten rett og tiltak uten datoer`() {
        val (pdlTransport, historikkTransport) = transports(
            arenaRadJson(arenaId = 142536),
            arenaRadJson(arenaId = 2, tiltakskode = "LONNTIL"),
            arenaRadJson(arenaId = 3, status = "SOKT_INN", startDato = null, sluttDato = null),
        )
        val correlationId = CorrelationId.generate()

        runTest {
            val tiltaksdeltakelser = client(pdlTransport, historikkTransport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = correlationId,
            ).getOrNull().shouldNotBeNull()

            // Arena-deltakelsen bærer kildens TA-id, ikke kontraktens uuid.
            tiltaksdeltakelser.value.map { it.eksternDeltakelseId } shouldBe listOf("TA142536")
            val deltakelse = tiltaksdeltakelser.value.single()
            // fixedClock er 1. januar 2025, så GJENNOMFORES med start i 2024 er i gang.
            deltakelse.deltakelseStatus shouldBe TiltakDeltakerstatus.Deltar
            deltakelse.kilde shouldBe Tiltakskilde.Arena
            deltakelse.deltidsprosentGjennomforing shouldBe 50.0
            deltakelse.rettPåTiltakspenger shouldBe true
        }

        val kall = historikkTransport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$historikkBaseUrl/api/v1/historikk"
        kall.request.headers().allValues("Nav-Call-Id") shouldBe listOf(correlationId.value)
        kall.bodyTekst shouldContain fnr.verdi
    }

    @Test
    fun `hentTiltaksdeltakelser - deltakelse med ukjent kildestatus utelates fra uttrekket`() {
        val (pdlTransport, historikkTransport) = transports(arenaRadJson(status = "HELT_NY_STATUS"))

        runTest {
            val tiltaksdeltakelser = client(pdlTransport, historikkTransport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = CorrelationId.generate(),
            ).getOrNull().shouldNotBeNull()

            tiltaksdeltakelser.value shouldBe emptyList()
        }
    }

    @Test
    fun `hentTiltaksdeltakelser - feil hos tiltakshistorikk gir Left med KallFeilet`() {
        val pdlTransport = FakeHttpTransport().apply { leggIKøJson(pdlJson(fnr)) }
        val historikkTransport = FakeHttpTransport().apply {
            leggIKøStatusForAlleForsøk(statusCode = 500, body = "feil", maksForsøk = 3)
        }

        runTest {
            val feil = client(pdlTransport, historikkTransport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = CorrelationId.generate(),
            ).leftOrNull().shouldNotBeNull()

            feil.shouldBeInstanceOf<KunneIkkeHenteTiltakshistorikk.KallFeilet>()
                .httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        }
    }

    @Test
    fun `hentTiltaksdeltakelser - feil i PDL-oppslaget gir Left med IdentoppslagFeilet`() {
        val pdlTransport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil") }
        val historikkTransport = FakeHttpTransport()

        runTest {
            val feil = client(pdlTransport, historikkTransport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = CorrelationId.generate(),
            ).leftOrNull().shouldNotBeNull()

            feil.shouldBeInstanceOf<KunneIkkeHenteTiltakshistorikk.IdentoppslagFeilet>()
        }
    }

    @Test
    fun `hentTiltaksdeltakelser - graphql-feil i PDL faller tilbake til innsendt fnr`() {
        val pdlTransport = FakeHttpTransport().apply {
            leggIKøJson("""{"data": null, "errors": [{"message": "feil", "locations": null, "path": null, "extensions": null}]}""")
        }
        val historikkTransport = FakeHttpTransport().apply { leggIKøJson(responsJson(listOf(arenaRadJson()))) }

        runTest {
            client(pdlTransport, historikkTransport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = CorrelationId.generate(),
            ).getOrNull().shouldNotBeNull()
        }

        historikkTransport.mottatteKall.single().bodyTekst shouldContain fnr.verdi
    }

    @Test
    fun `hentTiltaksdeltakelserMedArrangørnavn - maskerer arrangørnavn ved adressebeskyttelse`() {
        val (pdlTransport, historikkTransport) = transports(arenaRadJson())

        runTest {
            val tiltaksdeltakelser = client(pdlTransport, historikkTransport).hentTiltaksdeltakelserMedArrangørnavn(
                fnr = fnr,
                harAdressebeskyttelse = true,
                correlationId = CorrelationId.generate(),
            ).getOrNull().shouldNotBeNull()

            tiltaksdeltakelser.single().visningsnavn shouldBe "Oppfølging"
        }
    }

    @Test
    fun `hentTiltaksdeltakelserMedArrangørnavn - viser tittelen når bruker ikke har adressebeskyttelse`() {
        val (pdlTransport, historikkTransport) = transports(arenaRadJson())

        runTest {
            val tiltaksdeltakelser = client(pdlTransport, historikkTransport).hentTiltaksdeltakelserMedArrangørnavn(
                fnr = fnr,
                harAdressebeskyttelse = false,
                correlationId = CorrelationId.generate(),
            ).getOrNull().shouldNotBeNull()

            tiltaksdeltakelser.single().visningsnavn shouldBe "Oppfølging hos Arrangør AS"
        }
    }
}
