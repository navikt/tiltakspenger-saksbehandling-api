package no.nav.tiltakspenger.saksbehandling

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.ConsoleAppender
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.slf4j.Logger

class MaskedLogTest {
    @Test
    fun `skal maskere fnr`() {
        val loggedLines = mutableListOf<String>()
        val logger = createLogger(loggedLines::add)
        logger.info("Skal maskere: 12345678912")
        JSONObject(loggedLines[0]).getString("message") shouldBe "Skal maskere: ***********"
        logger.info("Skal ikke maskere: 1234567890123")
        JSONObject(loggedLines[1]).getString("message") shouldBe "Skal ikke maskere: 1234567890123"
        logger.info("Skal maskere: 12345678901er")
        JSONObject(loggedLines[2]).getString("message") shouldBe "Skal maskere: ***********er"
        logger.info("Skal maskere: e12345678901e")
        JSONObject(loggedLines[3]).getString("message") shouldBe "Skal maskere: e***********e"
        logger.info("Skal ikke maskere: 123456 12345")
        JSONObject(loggedLines[4]).getString("message") shouldBe "Skal ikke maskere: 123456 12345"
        logger.info("Skal ikke maskere: 123456  12345")
        JSONObject(loggedLines[5]).getString("message") shouldBe "Skal ikke maskere: 123456  12345"
    }

    private fun createLogger(onLog: (String) -> Unit): Logger {
        val appenderXml = Regex("""<appender\s+name="STDOUT_JSON".*?</appender>""", RegexOption.DOT_MATCHES_ALL)
            .find(this::class.java.classLoader.getResource("logback.xml")!!.readText())!!.value
        val context = LoggerContext().apply { mdcAdapter = LogbackMDCAdapter() }
        JoranConfigurator().apply {
            this.context = context
            doConfigure("""<configuration>$appenderXml<root level="DEBUG"><appender-ref ref="STDOUT_JSON"/></root></configuration>""".byteInputStream())
        }
        val encoder = (
            context.getLogger(Logger.ROOT_LOGGER_NAME)
                .getAppender("STDOUT_JSON") as ConsoleAppender<ILoggingEvent>
            ).also { it.stop() }.encoder
        return context.getLogger("test").apply {
            addAppender(
                object : AppenderBase<ILoggingEvent>() {
                    override fun append(event: ILoggingEvent) = onLog(String(encoder.encode(event)))
                }.apply {
                    this.context = context
                    start()
                },
            )
        }
    }
}
