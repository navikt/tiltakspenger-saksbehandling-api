package no.nav.tiltakspenger.saksbehandling.infra.http

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.assertSoftly
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration

/**
 * Verifiserer at [loggFeil] håndterer både feil med og uten throwable uten å kaste.
 * Logglinjene i seg selv testes ikke (log-output er sideeffekt), men begge grenene av [HttpKlientError.throwableOrNull] må kjøres.
 */
internal class HttpKlientErrorLoggingTest {

    private val logger = KotlinLogging.logger("test")

    private fun metadata(statusCode: Int?) = HttpKlientMetadata(
        rawRequestString = "POST http://test/endepunkt",
        rawResponseString = "noe gikk galt",
        requestHeaders = emptyMap(),
        responseHeaders = emptyMap(),
        statusCode = statusCode,
        attempts = 1,
        attemptDurations = emptyList(),
        totalDuration = Duration.ZERO,
        tidsstempler = HttpKlientTidsstempler.INGEN,
    )

    @Test
    fun `logger feil uten throwable`() {
        val error = HttpKlientError.UventetStatus(
            statusCode = 500,
            body = "noe gikk galt",
            metadata = metadata(statusCode = 500),
        )

        assertSoftly {
            error.loggFeil(logger, "sending til testtjeneste", "Sak 123")
        }
    }

    @Test
    fun `logger feil med throwable`() {
        val error = HttpKlientError.NetworkError(
            throwable = IOException("connection reset"),
            metadata = metadata(statusCode = null),
        )

        assertSoftly {
            error.loggFeil(logger, "sending til testtjeneste", "Sak 123")
        }
    }
}
