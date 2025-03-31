package no.nav.tiltakspenger.saksbehandling.clients.utbetaling

import arrow.core.right
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.saksbehandling.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingHttpClient
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

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
                sakId = sakId,
                saksnummer = saksnummer,
                vedtakId = vedtakId,
                opprettet = nå(fixedClock),
                sendtTilUtbetalingstidspunkt = nå(fixedClock.plus(1, ChronoUnit.SECONDS)),
                forsøkshistorikk = Forsøkshistorikk.førsteForsøk(fixedClock.plus(2, ChronoUnit.SECONDS)),
            )
            runTest {
                pdlClient.hentUtbetalingsstatus(utbetaling) shouldBe Utbetalingsstatus.OkUtenUtbetaling.right()
            }
        }
    }
}
