package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.OppsummeringGenerator
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsfeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringstestdata
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.HelvedPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.HelvedUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.tilHelvedUtbetalingJson
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID

class SimuleringV3HttpKlientTest {
    private val baseUrl = "http://simulering.helved"
    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = no.nav.tiltakspenger.libs.common.AccessToken("token", Instant.MAX)
    }

    @Test
    fun `standard transport sender forespørsel til direkte endepunkt`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/api/simulering"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = infoRespons("OK_UTEN_ENDRING", "TILTAKSPENGER")
            }

            runTest {
                val grunnlag = standardGrunnlag()
                val respons = SimuleringV3HttpKlient(
                    baseUrl = wiremock.baseUrl(),
                    clock = fixedClock,
                    authTokenProvider = authTokenProvider,
                ).simuler(
                    sakId = grunnlag.sakId,
                    saksnummer = grunnlag.saksnummer,
                    behandlingId = grunnlag.behandlingId,
                    fnr = grunnlag.fnr,
                    beregning = grunnlag.beregning,
                    forrigeUtbetalingJson = grunnlag.forrigeUtbetalingJson,
                    meldeperiodeKjeder = grunnlag.meldeperiodeKjeder,
                    saksbehandler = grunnlag.saksbehandler,
                    brukersNavkontor = grunnlag.brukersNavkontor,
                ).getOrNull().shouldNotBeNull()

                respons.simulering shouldBe Simulering.IngenEndring(nå(fixedClock))
            }
        }
    }

    @Test
    fun `sender tørrkjøring med sakstilstand til direkte endepunkt`() = runTest {
        val fnr = ObjectMother.gyldigFnr()
        val saksnummer = ObjectMother.nesteSaksnummer()
        val forrigePeriode = Periode(2.januar(2023), 15.januar(2023))
        val gjeldendePeriode = Periode(16.januar(2023), 29.januar(2023))
        val forrigeVedtak = ObjectMother.meldekortvedtak(
            periode = forrigePeriode,
            fnr = fnr,
            saksnummer = saksnummer,
        )
        val vedtak = ObjectMother.meldekortvedtak(
            periode = gjeldendePeriode,
            fnr = fnr,
            saksnummer = saksnummer,
        )
        val forrigeJson = forrigeVedtak.utbetaling.tilHelvedUtbetalingJson(null)
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                json = infoRespons(
                    status = "OK_UTEN_ENDRING",
                    fagsystem = "TILTAKSPENGER",
                ),
            )
        }

        val respons = client(transport).simuler(
            sakId = vedtak.sakId,
            saksnummer = saksnummer,
            behandlingId = vedtak.id,
            fnr = fnr,
            beregning = vedtak.utbetaling.beregning,
            forrigeUtbetalingJson = forrigeJson,
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            saksbehandler = vedtak.utbetaling.saksbehandler,
            brukersNavkontor = vedtak.utbetaling.brukerNavkontor,
        ).getOrNull().shouldNotBeNull()

        respons.simulering shouldBe Simulering.IngenEndring(nå(fixedClock))
        respons.originalResponseBody shouldBe infoRespons("OK_UTEN_ENDRING", "TILTAKSPENGER")

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe URI.create("$baseUrl/api/simulering").toString()
        kall.request.headers().firstValue("Authorization").orElse(null) shouldBe "Bearer token"
        val transactionId = kall.request.headers().firstValue("Transaction-ID").orElse(null).shouldNotBeNull()
        UUID.fromString(transactionId).toString() shouldBe transactionId
        kall.request.timeout().orElseThrow() shouldBe Duration.ofSeconds(130)

        val request = deserialize<HelvedUtbetalingDTO>(kall.bodyTekst)
        request.sakId shouldBe saksnummer.toString()
        request.behandlingId shouldBe vedtak.id.uuidPart()
        request.dryrun shouldBe true
        request.personident shouldBe fnr.verdi
        request.vedtakstidspunkt shouldBe nå(fixedClock)
        request.saksbehandler shouldBe vedtak.utbetaling.saksbehandler
        request.beslutter shouldBe null

        val forrigeRequest = deserialize<HelvedUtbetalingDTO>(forrigeJson)
        val forventedePerioder = vedtak.utbetaling.beregning.tilUtbetalingerDTO(
            brukersNavkontor = vedtak.utbetaling.brukerNavkontor,
            forrigeUtbetalingJson = forrigeJson,
        ).map {
            HelvedPeriodeDTO(
                meldeperiode = it.stønadsdata.meldekortId,
                fom = it.fraOgMedDato,
                tom = it.tilOgMedDato,
                betalendeEnhet = it.stønadsdata.brukersNavKontor,
                barnetillegg = it.stønadsdata.barnetillegg,
                beløp = it.beløp,
                stønad = it.stønadsdata.stønadstype.name,
            )
        }
        request.perioder shouldBe forventedePerioder
        request.perioder.filter { it.meldeperiode == forrigeRequest.perioder.first().meldeperiode } shouldBe
            forrigeRequest.perioder
        request.perioder.map { it.meldeperiode }.distinct() shouldBe listOf(
            forrigeRequest.perioder.first().meldeperiode,
            MeldeperiodeKjedeId.fraPeriode(gjeldendePeriode).verdi,
        )
    }

    @Test
    fun `info uten endring blir en simulering uten endring`() = runTest {
        val responseBody = infoRespons("OK_UTEN_ENDRING", "TILTAKSPENGER")
        val transport = FakeHttpTransport().apply { leggIKøJson(json = responseBody) }

        val result = simuler(transport).getOrNull().shouldNotBeNull()

        result.simulering shouldBe Simulering.IngenEndring(nå(fixedClock))
        result.originalResponseBody shouldBe responseBody
    }

    @Test
    fun `v1-svaret mapper alle tiltakspenger-fagområder og filtrerer andre fagsystemer`() = runTest {
        listOf("TILTPENG", "TPARENA", "MTPARENA").forEach { fagområde ->
            val postings = listOf(
                Testpostering(
                    fagområde = fagområde,
                    sakId = Simuleringstestdata.saksnummer.verdi,
                    type = "YTELSE",
                    klassekode = "TPTPAFT",
                    beløp = 456,
                ),
                Testpostering(
                    fagområde = "DPARENA",
                    sakId = ObjectMother.nesteSaksnummer().verdi,
                    type = "YTELSE",
                    klassekode = "DPORAS",
                    beløp = 999,
                ),
                Testpostering(
                    fagområde = fagområde,
                    sakId = Simuleringstestdata.saksnummer.verdi,
                    type = "FEILUTBETALING",
                    klassekode = OppsummeringGenerator.KLASSEKODE_FEILUTBETALING,
                    beløp = 100,
                ),
            )
            val transport = FakeHttpTransport().apply {
                leggIKøJson(json = v1Respons(Simuleringstestdata.fnr.verdi, postings))
            }

            val result = simuler(transport).getOrNull().shouldNotBeNull()
            val endring = result.simulering.shouldBeInstanceOf<Simulering.Endring>()

            endring.nyUtbetaling shouldBe 356
            endring.totalFeilutbetaling shouldBe 100
            endring.harFeilutbetaling shouldBe true
            endring.simuleringPerMeldeperiode.flatMap { it.posteringer }.map { it.fagområde }.distinct() shouldBe
                listOf("TILTAKSPENGER")
        }
    }

    @Test
    fun `v1-tomt svar validerer personen før ingen endring`() = runTest {
        val annenFnr = ObjectMother.gyldigFnr()
        val transport = FakeHttpTransport().apply {
            leggIKøJson(json = v1Respons(gjelderId = annenFnr.verdi, posteringer = emptyList()))
        }

        val feil = simuler(transport).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.UgyldigSimulering>()

        feil.feil.shouldBeInstanceOf<Simuleringsfeil.GjelderAnnenPerson>()
    }

    @Test
    fun `v1-svar for annen sak gir en typet simuleringsfeil`() = runTest {
        val annenSaksnummer = ObjectMother.nesteSaksnummer()
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                json = v1Respons(
                    gjelderId = Simuleringstestdata.fnr.verdi,
                    posteringer = listOf(
                        Testpostering(
                            fagområde = "TILTPENG",
                            sakId = annenSaksnummer.verdi,
                            type = "YTELSE",
                            klassekode = "TPTPAFT",
                            beløp = 456,
                        ),
                    ),
                ),
            )
        }

        val feil = simuler(transport).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.UgyldigSimulering>()

        feil.feil.shouldBeInstanceOf<Simuleringsfeil.GjelderAnnenSak>()
    }

    @Test
    fun `v1-feil maskerer personidentitet i feilmeldingen`() = runTest {
        val fnr = Simuleringstestdata.fnr
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                json = v1Respons(
                    gjelderId = fnr.verdi,
                    posteringer = listOf(
                        Testpostering(
                            fagområde = "UKJENT",
                            sakId = Simuleringstestdata.saksnummer.verdi,
                            type = "YTELSE",
                            klassekode = "TPTPAFT",
                            beløp = 456,
                        ),
                    ),
                ),
            )
        }

        val feil = simuler(transport)
            .leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>()
        val feilmelding = feil.toString()

        feilmelding shouldNotContain fnr.verdi
        feilmelding shouldContain "maskert"
    }

    @Test
    fun `ukjent body type fagområde eller info-status gir en HTTP-feil med metadata`() = runTest {
        val bodies = listOf(
            """{"@type":"v2","perioder":[]}""",
            """{"@type":"info","status":"NY_STATUS","fagsystem":"TILTAKSPENGER","message":"ukjent"}""",
            """{"@type":"info","status":"OK_UTEN_ENDRING","fagsystem":"DAGPENGER","message":"annet"}""",
            v1Respons(
                gjelderId = Simuleringstestdata.fnr.verdi,
                posteringer = listOf(
                    Testpostering(
                        fagområde = "NYTT_FAGOMRÅDE",
                        sakId = Simuleringstestdata.saksnummer.verdi,
                        type = "YTELSE",
                        klassekode = "TPTPAFT",
                        beløp = 456,
                    ),
                ),
            ),
        )

        bodies.forEach { responseBody ->
            val transport = FakeHttpTransport().apply { leggIKøJson(json = responseBody) }
            val feil = simuler(transport).leftOrNull().shouldNotBeNull()
            feil.shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>()
        }
    }

    @Test
    fun `feilede info-statuser gir ikke suksessformet simulering`() = runTest {
        listOf("FEILET", "UTILGJENGELIG", "UGYLDIG_REQUEST").forEach { status ->
            val transport = FakeHttpTransport().apply {
                leggIKøJson(json = infoRespons(status, "TILTAKSPENGER"))
            }

            simuler(transport).leftOrNull()
                .shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>()
        }
    }

    @Test
    fun `HTTP 408 og transporttimeout blir timeout, mens 503 ikke betyr stengt`() = runTest {
        val statusTransport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 408, body = "timeout", contentType = "text/plain")
        }
        simuler(statusTransport).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.Timeout>()
            .feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 408

        val transportTimeout = FakeHttpTransport().apply {
            leggIKøKast(java.net.http.HttpTimeoutException("simulert timeout"))
        }
        simuler(transportTimeout).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.Timeout>()
            .feil.shouldBeInstanceOf<HttpKlientError.Timeout>()

        val unavailable = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 503, body = "utilgjengelig", contentType = "text/plain")
        }
        simuler(unavailable).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>()
            .feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 503
    }

    @Test
    fun `ugyldig JSON og manglende type gir typet HTTP-feil`() = runTest {
        val ugyldigJson = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 200, body = "ikke json", contentType = "application/json")
        }
        simuler(ugyldigJson).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>()
            .feil.shouldBeInstanceOf<HttpKlientError.DeserializationError>()

        val manglendeType = FakeHttpTransport().apply { leggIKøJson(json = """{"oppsummeringer":[]}""") }
        simuler(manglendeType).leftOrNull()
            .shouldBeInstanceOf<KunneIkkeSimulere.UkjentFeil>()
    }

    private fun client(transport: FakeHttpTransport) = SimuleringV3HttpKlient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = fixedClock,
        transport = transport,
    )

    private suspend fun simuler(
        transport: FakeHttpTransport,
        grunnlag: Simuleringsgrunnlag = standardGrunnlag(),
    ) = client(transport).simuler(
        sakId = grunnlag.sakId,
        saksnummer = grunnlag.saksnummer,
        behandlingId = grunnlag.behandlingId,
        fnr = grunnlag.fnr,
        beregning = grunnlag.beregning,
        forrigeUtbetalingJson = grunnlag.forrigeUtbetalingJson,
        meldeperiodeKjeder = grunnlag.meldeperiodeKjeder,
        saksbehandler = grunnlag.saksbehandler,
        brukersNavkontor = grunnlag.brukersNavkontor,
    )

    private fun standardGrunnlag() = Simuleringsgrunnlag(
        sakId = Simuleringstestdata.sakId,
        saksnummer = Simuleringstestdata.saksnummer,
        behandlingId = MeldekortId.random(),
        fnr = Simuleringstestdata.fnr,
        beregning = ObjectMother.lagBeregning(startDato = Simuleringstestdata.førsteDag),
        meldeperiodeKjeder = Simuleringstestdata.meldeperiodeKjeder,
        saksbehandler = "Z123456",
        brukersNavkontor = ObjectMother.navkontor(),
    )

    private fun infoRespons(status: String, fagsystem: String) =
        """{"@type":"info","status":"$status","fagsystem":"$fagsystem","message":"simuleringsresultat"}"""

    private fun v1Respons(
        gjelderId: String,
        posteringer: List<Testpostering>,
    ): String {
        val perioder = if (posteringer.isEmpty()) {
            "[]"
        } else {
            val dato = Simuleringstestdata.førsteDag
            val posteringerJson = posteringer.joinToString(",") {
                """{"fagområde":"${it.fagområde}","sakId":"${it.sakId}","fom":"$dato","tom":"$dato","beløp":${it.beløp},"type":"${it.type}","klassekode":"${it.klassekode}"}"""
            }
            """[{"fom":"$dato","tom":"$dato","posteringer":[$posteringerJson]}]"""
        }

        return """{"@type":"v1","oppsummeringer":[],"detaljer":{"gjelderId":"$gjelderId","datoBeregnet":"${Simuleringstestdata.førsteDag}","totalBeløp":0,"perioder":$perioder}}"""
    }

    private data class Testpostering(
        val fagområde: String,
        val sakId: String,
        val type: String,
        val klassekode: String,
        val beløp: Int,
    )

    private data class Simuleringsgrunnlag(
        val sakId: SakId,
        val saksnummer: Saksnummer,
        val behandlingId: MeldekortId,
        val fnr: Fnr,
        val beregning: no.nav.tiltakspenger.saksbehandling.beregning.Beregning,
        val forrigeUtbetalingJson: String? = null,
        val meldeperiodeKjeder: MeldeperiodeKjeder,
        val saksbehandler: String,
        val brukersNavkontor: no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor,
    )
}
