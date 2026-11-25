package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import arrow.core.right
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaSisteDagMedRettEtter
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForSisteDagMedRett
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaUtbetalingerForMåned
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class TiltakspengerArenaHttpClientTest {
    private val baseUrl = "http://arena.test"
    private val correlationId = CorrelationId.generate()

    private val fnr = Fnr.random()
    private val periode = no.nav.tiltakspenger.libs.periode.Periode(
        fraOgMed = LocalDate.of(2024, 1, 1),
        tilOgMed = LocalDate.of(2024, 1, 31),
    )

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = TiltakspengerArenaHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        transport = transport,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        TiltakspengerArenaHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `henter vedtak - 200 gir liste og POSTer til arena-endepunktet`() {
        val vedtak = ArenaTPVedtak(
            fraOgMed = LocalDate.of(2024, 1, 1),
            tilOgMed = LocalDate.of(2024, 1, 31),
            rettighet = ArenaTPVedtak.Rettighet.TILTAKSPENGER,
            vedtakId = 42L,
        )
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                listOf(
                    ArenaTPVedtakDto(
                        fraOgMed = vedtak.fraOgMed,
                        tilOgMed = vedtak.tilOgMed,
                        rettighet = vedtak.rettighet,
                        vedtakId = vedtak.vedtakId,
                    ),
                ),
                statusCode = 200,
            )
        }

        runTest {
            client(transport).hentTiltakspengevedtakFraArena(fnr, periode, correlationId) shouldBe listOf(vedtak).right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/azure/tiltakspenger/vedtak"
    }

    @Test
    fun `henter vedtak - uventet status gir Left`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil", contentType = "text/plain") }

        runTest {
            val result = client(transport).hentTiltakspengevedtakFraArena(fnr, periode, correlationId)
            result.isLeft() shouldBe true
        }
    }

    @Test
    fun `henter statistikk per måned med GET og året som parameter`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                """[{"måned":"2026-02","sakerMedRett":5,"nyeRettigheter":1,"gjenopptak":2,"endringer":3,"stans":4}]""",
            )
        }

        runTest {
            client(transport).hentStatistikkPerMåned(2026) shouldBe listOf(
                ArenaStatistikkForMåned(YearMonth.of(2026, 2), 5, 1, 2, 3, 4),
            ).right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "GET"
        kall.uri.toString() shouldBe "$baseUrl/azure/tiltakspenger/statistikk/per-maned?aar=2026"
    }

    @Test
    fun `henter siste dag med rett med GET og datoen som parameter`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                """{"etter":"2024-01-01","perSisteDagMedRett":[{"sisteDagMedRett":"2026-12-15","saker":5,"personer":4},{"sisteDagMedRett":null,"saker":2,"personer":2}]}""",
            )
        }

        runTest {
            client(transport).hentSisteDagMedRettEtter(LocalDate.of(2024, 1, 1)) shouldBe
                ArenaSisteDagMedRettEtter(
                    etter = LocalDate.of(2024, 1, 1),
                    perSisteDagMedRett = listOf(
                        ArenaStatistikkForSisteDagMedRett(LocalDate.of(2026, 12, 15), 5, 4),
                        ArenaStatistikkForSisteDagMedRett(null, 2, 2),
                    ),
                ).right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "GET"
        kall.uri.toString() shouldBe "$baseUrl/azure/tiltakspenger/statistikk/siste-dag-med-rett?etter=2024-01-01"
    }

    @Test
    fun `henter utbetalinger per måned med GET og året som parameter`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                """[{"måned":"2026-02","personerMedUtbetaling":3,"dagerUtbetalt":25.50,"beløpGrunnsats":7267.50,"beløpBarnetillegg":530.00,"personerMedBeregnetIkkeUtbetalt":1,"beløpBeregnetIkkeUtbetalt":1425.00}]""",
            )
        }

        runTest {
            client(transport).hentUtbetalingerPerMåned(2026) shouldBe listOf(
                ArenaUtbetalingerForMåned(
                    måned = YearMonth.of(2026, 2),
                    personerMedUtbetaling = 3,
                    dagerUtbetalt = BigDecimal("25.50"),
                    beløpGrunnsats = BigDecimal("7267.50"),
                    beløpBarnetillegg = BigDecimal("530.00"),
                    personerMedBeregnetIkkeUtbetalt = 1,
                    beløpBeregnetIkkeUtbetalt = BigDecimal("1425.00"),
                ),
            ).right()
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "GET"
        kall.uri.toString() shouldBe "$baseUrl/azure/tiltakspenger/statistikk/utbetalinger-per-maned?aar=2026"
    }
}
