package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.right
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.saksbehandling.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

/**
 * Test for [UtbetalingHttpKlient]
 */
internal class UtbetalingHttpClientTest {
    @Test
    fun `bør håndtere OK_UTEN_UTBETALING fra helved`() {
        val saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001")
        val utbetalingId = UtbetalingId.random()
        val sakId = SakId.random()
        val response = """"OK_UTEN_UTBETALING""""
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/api/iverksetting/${saksnummer.verdi}/${utbetalingId.uuidPart()}/status"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
                body = response
            }

            val clock = fixedClock
            val pdlClient = UtbetalingHttpKlient(
                baseUrl = wiremock.baseUrl(),
                getToken = { ObjectMother.accessToken() },
                clock = clock,
            )
            val utbetaling = UtbetalingDetSkalHentesStatusFor(
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
                sendtTilUtbetalingstidspunkt = nå(clock.plus(1, ChronoUnit.SECONDS)),
                forsøkshistorikk = Forsøkshistorikk.opprett(clock = clock.plus(2, ChronoUnit.SECONDS)),
                utbetalingId = utbetalingId,
            )
            runTest {
                pdlClient.hentUtbetalingsstatus(utbetaling) shouldBe Utbetalingsstatus.OkUtenUtbetaling.right()
            }
        }
    }
}
