package no.nav.tiltakspenger.saksbehandling.felles

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.Level
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test

class ErrorEveryNLoggerTest {

    @Test
    fun `Skal logge selve meldingen på default-nivå uten throwable`() {
        val logger = mockk<KLogger>(relaxed = true)
        val block = slot<KLoggingEventBuilder.() -> Unit>()
        every { logger.at(Level.WARN, any(), capture(block)) } just Runs

        ErrorEveryNLogger(logger, 3).log { "melding uten throwable" }

        val hendelse = KLoggingEventBuilder().apply(block.captured)
        hendelse.message shouldBe "melding uten throwable"
    }

    @Test
    fun `Skal logge selve meldingen og årsaken på default-nivå med throwable`() {
        val logger = mockk<KLogger>(relaxed = true)
        val block = slot<KLoggingEventBuilder.() -> Unit>()
        every { logger.at(Level.WARN, any(), capture(block)) } just Runs
        val feil = RuntimeException("boom")

        ErrorEveryNLogger(logger, 3).log(feil) { "melding med throwable" }

        val hendelse = KLoggingEventBuilder().apply(block.captured)
        hendelse.message shouldBe "melding med throwable"
        hendelse.cause shouldBe feil
    }

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
