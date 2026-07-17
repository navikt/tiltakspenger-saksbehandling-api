package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.nonEmptyListOf
import io.github.oshai.kotlinlogging.KLogger
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test

class KanIkkeIverksetteUtbetalingLoggTest {

    @Test
    fun `SimuleringMangler logges som error med utfallet stemplet på meldingen`() {
        val logger = mockk<KLogger>(relaxed = true)
        val melding = slot<() -> Any?>()
        every { logger.error(capture(melding)) } just Runs

        KanIkkeIverksetteUtbetaling.SimuleringMangler.logg(logger) { "kontekst" }

        melding.captured() shouldBe "kontekst - KanIkkeIverksetteUtbetaling.SimuleringMangler"
        verify(exactly = 0) { logger.warn(any<() -> Any?>()) }
    }

    @Test
    fun `KontrollSimuleringHarEndringer logges som warn med ulikhetene i meldingen`() {
        val logger = mockk<KLogger>(relaxed = true)
        val melding = slot<() -> Any?>()
        every { logger.warn(capture(melding)) } just Runs

        KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer(
            nonEmptyListOf("første ulikhet", "andre ulikhet"),
        ).logg(logger) { "kontekst" }

        melding.captured() shouldBe "kontekst - KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer(første ulikhet; andre ulikhet)"
        verify(exactly = 0) { logger.error(any<() -> Any?>()) }
    }

    @Test
    fun `øvrige utfall logges som warn`() {
        val logger = mockk<KLogger>(relaxed = true)

        listOf(
            KanIkkeIverksetteUtbetaling.JusteringStøttesIkke,
            KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling,
            KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering,
        ).forEach { it.logg(logger) { "kontekst" } }

        verify(exactly = 3) { logger.warn(any<() -> Any?>()) }
        verify(exactly = 0) { logger.error(any<() -> Any?>()) }
    }
}
