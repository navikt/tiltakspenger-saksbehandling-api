package no.nav.tiltakspenger.saksbehandling.felles

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.Level
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ErrorEveryNLoggerTest {

    @Test
    fun `Skal logge hvert tredje kall som error`() {
        val logger = mockk<KLogger>(relaxed = true)

        val errorEveryNLogger = ErrorEveryNLogger(logger, 3)

        repeat(2) {
            errorEveryNLogger.log { "logger noe" }
        }

        verify(exactly = 0) { logger.error(any<() -> String>()) }
        verify(exactly = 2) {
            logger.at(Level.WARN, any(), any())
        }

        errorEveryNLogger.log { "oh noes" }

        verify(exactly = 1) { logger.error(any<() -> String>()) }
        verify(exactly = 2) {
            logger.at(Level.WARN, any(), any())
        }

        repeat(10) {
            errorEveryNLogger.log { "logger mer" }
        }

        verify(exactly = 4) { logger.error(any<() -> String>()) }
        verify(exactly = 9) {
            logger.at(Level.WARN, any(), any())
        }
    }

    @Test
    fun `Skal resette teller`() {
        val logger = mockk<KLogger>(relaxed = true)

        val errorEveryNLogger = ErrorEveryNLogger(logger, 3)

        repeat(2) {
            errorEveryNLogger.log { "logger noe" }
        }

        verify(exactly = 0) { logger.error(any<() -> String>()) }
        verify(exactly = 2) {
            logger.at(Level.WARN, any(), any())
        }

        errorEveryNLogger.reset()

        errorEveryNLogger.log { "oh noes" }

        verify(exactly = 0) { logger.error(any<() -> String>()) }
        verify(exactly = 3) {
            logger.at(Level.WARN, any(), any())
        }

        repeat(10) {
            errorEveryNLogger.log { "logger mer" }
        }

        verify(exactly = 3) { logger.error(any<() -> String>()) }
        verify(exactly = 10) {
            logger.at(Level.WARN, any(), any())
        }
    }
}
