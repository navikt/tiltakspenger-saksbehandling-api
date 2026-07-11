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
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientFake
import no.nav.tiltakspenger.libs.httpklient.HttpMethod
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

    private fun client(httpKlient: HttpKlientFake) = UtbetalingHttpKlient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = fixedClock,
        httpKlient = httpKlient,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        UtbetalingHttpKlient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = fixedClock,
        )
    }

    @Test
    fun `iverksett - 202 Accepted gir SendtUtbetaling`() {
        val httpKlient = HttpKlientFake().apply { enqueueStringResponse(body = "mottatt", statusCode = 202) }
        val utbetaling = ObjectMother.utbetaling()
        val correlationId = CorrelationId.generate()

        runTest {
            val sendtUtbetaling = client(httpKlient).iverksett(utbetaling, null, correlationId).getOrNull().shouldNotBeNull()
            sendtUtbetaling.request shouldBe utbetaling.toUtbetalingRequestDTO(null)
            sendtUtbetaling.response shouldBe "mottatt"
            sendtUtbetaling.responseStatus shouldBe 202
            sendtUtbetaling.alleredeMottattTidligere shouldBe false
        }

        val request = httpKlient.requests.single()
        request.method shouldBe HttpMethod.POST
        request.uri.toString() shouldBe "$baseUrl/api/iverksetting/v2"
        request.headers["Nav-Call-Id"] shouldBe listOf(correlationId.value)
    }

    @Test
    fun `iverksett - 409 med dedup-melding behandles som suksess`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueStringResponse(body = "Iverksettingen er allerede mottatt", statusCode = 409)
        }

        runTest {
            val sendtUtbetaling =
                client(httpKlient).iverksett(ObjectMother.utbetaling(), null, CorrelationId.generate()).getOrNull().shouldNotBeNull()
            sendtUtbetaling.responseStatus shouldBe 409
            sendtUtbetaling.alleredeMottattTidligere shouldBe true
        }
    }

    @Test
    fun `iverksett - 409 uten dedup-melding gir KunneIkkeUtbetale med UventetStatus`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueStringResponse(body = "en annen konflikt", statusCode = 409)
        }
        val utbetaling = ObjectMother.utbetaling()

        runTest {
            val kunneIkkeUtbetale =
                client(httpKlient).iverksett(utbetaling, null, CorrelationId.generate()).leftOrNull().shouldNotBeNull()
            kunneIkkeUtbetale.request shouldBe utbetaling.toUtbetalingRequestDTO(null)
            kunneIkkeUtbetale.response shouldBe "en annen konflikt"
            kunneIkkeUtbetale.responseStatus shouldBe 409
            kunneIkkeUtbetale.feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 409
        }
    }

    @Test
    fun `iverksett - 400 Bad Request gir KunneIkkeUtbetale med respons for notoritet`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 400, body = "ugyldig request") }
        val utbetaling = ObjectMother.utbetaling()

        runTest {
            val kunneIkkeUtbetale =
                client(httpKlient).iverksett(utbetaling, null, CorrelationId.generate()).leftOrNull().shouldNotBeNull()
            kunneIkkeUtbetale.request shouldBe utbetaling.toUtbetalingRequestDTO(null)
            kunneIkkeUtbetale.response shouldBe "ugyldig request"
            kunneIkkeUtbetale.responseStatus shouldBe 400
            kunneIkkeUtbetale.feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
        }
    }

    @Test
    fun `iverksett - timeout gir KunneIkkeUtbetale uten responsstatus`() {
        val httpKlient = HttpKlientFake().apply { enqueueTimeout() }

        runTest {
            val kunneIkkeUtbetale =
                client(httpKlient).iverksett(ObjectMother.utbetaling(), null, CorrelationId.generate()).leftOrNull().shouldNotBeNull()
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
                val httpKlient = HttpKlientFake().apply { enqueueResponse(body = iverksettStatus) }

                client(httpKlient).hentUtbetalingsstatus(utbetaling) shouldBe forventet.right()

                val request = httpKlient.requests.single()
                request.method shouldBe HttpMethod.GET
                request.uri.toString() shouldBe
                    "$baseUrl/api/iverksetting/${utbetaling.saksnummer.verdi}/${utbetaling.utbetalingId.uuidPart()}/status"
            }
        }
    }

    @Test
    fun `hentUtbetalingsstatus - feilstatus gir Left med HttpKlientError`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "feil") }

        runTest {
            val feil =
                client(httpKlient).hentUtbetalingsstatus(ObjectMother.utbetalingDetSkalHentesStatusFor()).leftOrNull().shouldNotBeNull()
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
        val httpKlient = HttpKlientFake().apply { enqueueStringResponse(body = helvedResponse, statusCode = 200) }

        runTest {
            simuler(httpKlient) shouldBe SimuleringMedMetadata(
                simulering = Simulering.IngenEndring(LocalDateTime.now(fixedClock)),
                originalResponseBody = helvedResponse,
            ).right()
        }

        val request = httpKlient.requests.single()
        request.method shouldBe HttpMethod.POST
        request.uri.toString() shouldBe "$baseUrl/api/simulering/v2"
    }

    @Test
    fun `simuler - 204 No Content gir IngenEndring`() {
        val httpKlient = HttpKlientFake().apply { enqueueStringResponse(body = "", statusCode = 204) }

        runTest {
            val simuleringMedMetadata = simuler(httpKlient).getOrNull().shouldNotBeNull()
            simuleringMedMetadata.simulering.shouldBeInstanceOf<Simulering.IngenEndring>()
        }
    }

    @Test
    fun `simuler - 503 Service Unavailable gir Stengt`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 503, body = "stengt") }

        runTest {
            simuler(httpKlient).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSimulere.Stengt>()
                .feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 503
        }
    }

    @Test
    fun `simuler - timeout gir Timeout`() {
        val httpKlient = HttpKlientFake().apply { enqueueTimeout() }

        runTest {
            simuler(httpKlient).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSimulere.Timeout>()
                .feil.shouldBeInstanceOf<HttpKlientError.Timeout>()
        }
    }

    @Test
    fun `simuler - respons som ikke lar seg parse gir UkjentFeil med DeserializationError`() {
        val httpKlient = HttpKlientFake().apply { enqueueStringResponse(body = "ikke json", statusCode = 200) }

        runTest {
            val feil = simuler(httpKlient).leftOrNull().shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>().feil
            feil.shouldBeInstanceOf<HttpKlientError.DeserializationError>().body shouldBe "ikke json"
        }
    }

    @Test
    fun `simuler - annen feilstatus gir UkjentFeil`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "feil") }

        runTest {
            val feil = simuler(httpKlient).leftOrNull().shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>().feil
            feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        }
    }

    private suspend fun simuler(httpKlient: HttpKlientFake): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return client(httpKlient).simuler(
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
