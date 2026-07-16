package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.right
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett.IverksettStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

internal class UtbetalingHttpKlientTest {
    private val baseUrl = "http://helved.test"

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = UtbetalingHttpKlient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = fixedClock,
        transport = transport,
    )

    @Test
    fun `bygger produksjonstransport når transport ikke sendes inn`() {
        UtbetalingHttpKlient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = fixedClock,
        )
    }

    @Test
    fun `iverksett - 202 Accepted gir SendtUtbetaling`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 202, body = "mottatt", contentType = "text/plain") }
        val utbetaling = ObjectMother.utbetaling()
        val correlationId = CorrelationId.generate()

        runTest {
            val sendtUtbetaling = client(transport).iverksett(utbetaling, null, correlationId).getOrNull().shouldNotBeNull()
            sendtUtbetaling.request shouldBe utbetaling.toUtbetalingRequestDTO(null)
            sendtUtbetaling.response shouldBe "mottatt"
            sendtUtbetaling.responseStatus shouldBe 202
            sendtUtbetaling.alleredeMottattTidligere shouldBe false
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/api/iverksetting/v2"
        kall.request.headers().allValues("Nav-Call-Id") shouldBe listOf(correlationId.value)
    }

    @Test
    fun `iverksett - 409 med dedup-melding behandles som suksess`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 409, body = "Iverksettingen er allerede mottatt", contentType = "text/plain")
        }

        runTest {
            val sendtUtbetaling =
                client(transport).iverksett(ObjectMother.utbetaling(), null, CorrelationId.generate()).getOrNull().shouldNotBeNull()
            sendtUtbetaling.responseStatus shouldBe 409
            sendtUtbetaling.alleredeMottattTidligere shouldBe true
        }
    }

    @Test
    fun `iverksett - 409 uten dedup-melding gir KunneIkkeUtbetale med UventetStatus`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 409, body = "en annen konflikt", contentType = "text/plain")
        }
        val utbetaling = ObjectMother.utbetaling()

        runTest {
            val kunneIkkeUtbetale =
                client(transport).iverksett(utbetaling, null, CorrelationId.generate()).leftOrNull().shouldNotBeNull()
            kunneIkkeUtbetale.request shouldBe utbetaling.toUtbetalingRequestDTO(null)
            kunneIkkeUtbetale.response shouldBe "en annen konflikt"
            kunneIkkeUtbetale.responseStatus shouldBe 409
            kunneIkkeUtbetale.feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 409
        }
    }

    @Test
    fun `iverksett - 400 Bad Request gir KunneIkkeUtbetale med respons for notoritet`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 400, body = "ugyldig request", contentType = "text/plain") }
        val utbetaling = ObjectMother.utbetaling()

        runTest {
            val kunneIkkeUtbetale =
                client(transport).iverksett(utbetaling, null, CorrelationId.generate()).leftOrNull().shouldNotBeNull()
            kunneIkkeUtbetale.request shouldBe utbetaling.toUtbetalingRequestDTO(null)
            kunneIkkeUtbetale.response shouldBe "ugyldig request"
            kunneIkkeUtbetale.responseStatus shouldBe 400
            kunneIkkeUtbetale.feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
        }
    }

    @Test
    fun `iverksett - timeout gir KunneIkkeUtbetale uten responsstatus`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.net.http.HttpTimeoutException("simulert timeout")) }

        runTest {
            val kunneIkkeUtbetale =
                client(transport).iverksett(ObjectMother.utbetaling(), null, CorrelationId.generate()).leftOrNull().shouldNotBeNull()
            kunneIkkeUtbetale.response shouldBe null
            kunneIkkeUtbetale.responseStatus shouldBe null
            kunneIkkeUtbetale.feil.shouldBeInstanceOf<HttpKlientError.Timeout>()
        }
    }

    @Test
    fun `hentUtbetalingsstatus - mapper alle statuser fra helved`() {
        val forventedeMappinger = listOf(
            IverksettStatus.SENDT_TIL_OPPDRAG to Utbetalingsstatus.SendtTilOppdrag,
            IverksettStatus.FEILET_MOT_OPPDRAG to Utbetalingsstatus.FeiletMotOppdrag,
            IverksettStatus.OK to Utbetalingsstatus.Ok,
            IverksettStatus.IKKE_PÅBEGYNT to Utbetalingsstatus.IkkePåbegynt,
            IverksettStatus.OK_UTEN_UTBETALING to Utbetalingsstatus.OkUtenUtbetaling,
        )
        val utbetaling = ObjectMother.utbetalingDetSkalHentesStatusFor()

        runTest {
            forventedeMappinger.forEach { (iverksettStatus, forventet) ->
                val transport = FakeHttpTransport().apply { leggIKøJson(iverksettStatus) }

                client(transport).hentUtbetalingsstatus(utbetaling) shouldBe forventet.right()

                val kall = transport.mottatteKall.single()
                kall.metode shouldBe "GET"
                kall.uri.toString() shouldBe
                    "$baseUrl/api/iverksetting/${utbetaling.saksnummer.verdi}/${utbetaling.utbetalingId.uuidPart()}/status"
            }
        }
    }

    @Test
    fun `hentUtbetalingsstatus - feilstatus gir Left med HttpKlientError`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil", contentType = "text/plain") }

        runTest {
            val feil =
                client(transport).hentUtbetalingsstatus(ObjectMother.utbetalingDetSkalHentesStatusFor()).leftOrNull().shouldNotBeNull()
            feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        }
    }

    @Test
    fun `simuler - 200 med simuleringsrespons parses`() {
        // language=json
        val helvedResponse = """
            {
              "oppsummeringer": [],
              "detaljer": {
                "gjelderId": "12345678910",
                "datoBeregnet": "2024-05-12",
                "totalBeløp": 0,
                "perioder": []
              }
            }
        """.trimIndent()
        val transport = FakeHttpTransport().apply { leggIKøJson(json = helvedResponse) }

        runTest {
            simuler(transport) shouldBe SimuleringMedMetadata(
                simulering = Simulering.IngenEndring(LocalDateTime.now(fixedClock)),
                originalResponseBody = helvedResponse,
            ).right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/api/simulering/v2"
    }

    @Test
    fun `simuler - 204 No Content gir IngenEndring`() {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 204) }

        runTest {
            val simuleringMedMetadata = simuler(transport).getOrNull().shouldNotBeNull()
            simuleringMedMetadata.simulering.shouldBeInstanceOf<Simulering.IngenEndring>()
        }
    }

    @Test
    fun `simuler - 503 Service Unavailable gir Stengt`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 503, body = "stengt", contentType = "text/plain") }

        runTest {
            simuler(transport).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSimulere.Stengt>()
                .feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 503
        }
    }

    @Test
    fun `simuler - timeout gir Timeout`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.net.http.HttpTimeoutException("simulert timeout")) }

        runTest {
            simuler(transport).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSimulere.Timeout>()
                .feil.shouldBeInstanceOf<HttpKlientError.Timeout>()
        }
    }

    @Test
    fun `simuler - respons som ikke lar seg parse gir UkjentFeil med DeserializationError`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 200, body = "ikke json") }

        runTest {
            val feil = simuler(transport).leftOrNull().shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>().feil
            feil.shouldBeInstanceOf<HttpKlientError.DeserializationError>().body shouldBe "ikke json"
        }
    }

    @Test
    fun `simuler - annen feilstatus gir UkjentFeil`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil", contentType = "text/plain") }

        runTest {
            val feil = simuler(transport).leftOrNull().shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>().feil
            feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        }
    }

    @Test
    fun `simuleringstidspunkt bruker responstidspunktet når det finnes, ellers klokka`() {
        val responsMottatt = LocalDateTime.now(fixedClock).minusMinutes(5)
        UtbetalingHttpKlient.simuleringstidspunkt(responsMottatt, fixedClock) shouldBe responsMottatt
        UtbetalingHttpKlient.simuleringstidspunkt(null, fixedClock) shouldBe LocalDateTime.now(fixedClock)
    }

    private suspend fun simuler(transport: FakeHttpTransport): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return client(transport).simuler(
            sakId = SakId.random(),
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = fixedClock),
            behandlingId = MeldekortId.random(),
            fnr = Fnr.random(),
            saksbehandler = "Z123456",
            beregning = ObjectMother.lagBeregning(),
            brukersNavkontor = ObjectMother.navkontor(),
            kanSendeInnHelgForMeldekort = false,
            forrigeUtbetalingJson = null,
            forrigeUtbetalingId = null,
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
        )
    }
}
