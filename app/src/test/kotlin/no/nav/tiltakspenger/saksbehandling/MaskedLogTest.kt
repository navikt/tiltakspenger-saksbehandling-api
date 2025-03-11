package no.nav.tiltakspenger.saksbehandling

import mu.KotlinLogging
import org.junit.jupiter.api.Test

class MaskedLogTest {
    @Test
    fun manuellTest() {
        System.setProperty("logback.configurationFile", "logback.xml")
        val log = KotlinLogging.logger {}
        log.info("12345678901")
        log.info("1234567890123")
        log.info("12345678901er")
        log.info("e12345678901e")
    }
}
