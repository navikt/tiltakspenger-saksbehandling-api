package no.nav.tiltakspenger.saksbehandling.clients.utbetaling

import arrow.core.right
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.withWireMockServer
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test

/**
 * Test for [UtbetalingHttpClient]
 */
internal class UtbetalingHttpClientTest {
    @Test
    fun `should handle `() {
        val saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001")
        val vedtakId = VedtakId.random()
        val sakId = SakId.random()
        val response = """"OK_UTEN_UTBETALING""""
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/api/iverksetting/${saksnummer.verdi}/${vedtakId.uuidPart()}/status"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = response
            }

            val pdlClient = UtbetalingHttpClient(
                baseUrl = wiremock.baseUrl(),
                getToken = { ObjectMother.accessToken() },
            )
            val utbetaling = UtbetalingDetSkalHentesStatusFor(
                saksnummer = saksnummer,
                vedtakId = vedtakId,
                sakId = sakId,
            )
            runTest {
                pdlClient.hentUtbetalingsstatus(utbetaling) shouldBe Utbetalingsstatus.OkUtenUtbetaling.right()
            }
        }
    }
}
