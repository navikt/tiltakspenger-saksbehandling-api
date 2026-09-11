package no.nav.tiltakspenger.saksbehandling

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.spi.FilterReply
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.logstash.logback.appender.LogstashTcpSocketAppender
import no.nav.tiltakspenger.libs.json.objectMapper
import org.junit.jupiter.api.Test
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets

class MaskedLogTest {
    @Test
    fun `produksjons-logback maskerer og ruter team logs riktig`() {
        ServerSocket(0, 50, InetAddress.getByName(LOKAL_LOOPBACK_ADRESSE)).use { serverSocket ->
            serverSocket.soTimeout = 5_000
            val context = LoggerContext().apply { mdcAdapter = LogbackMDCAdapter() }
            try {
                val konfigurasjon = isolertKonfigurasjonFraProduksjonsfilen(
                    port = serverSocket.localPort,
                    lokalAdresse = LOKAL_LOOPBACK_ADRESSE,
                )
                JoranConfigurator().apply {
                    this.context = context
                    doConfigure(konfigurasjon.byteInputStream())
                }

                val rootLogger = context.getLogger(ROOT_LOGGER_NAME)
                val teamLogger = context.getLogger("team-logs-logger")
                val teamLogsAppender = teamLogger.getAppender("team-logs") as LogstashTcpSocketAppender
                val stdoutAppender = rootLogger.getAppender("STDOUT_JSON") as ConsoleAppender<ILoggingEvent>
                val consoleOutput = ByteArrayOutputStream()
                stdoutAppender.outputStream = consoleOutput

                teamLogger.level shouldBe Level.DEBUG
                teamLogger.isAdditive shouldBe false
                (teamLogger.getAppender("team-logs") === teamLogsAppender) shouldBe true
                teamLogger.getAppender("STDOUT_JSON") shouldBe null

                teamLogsAppender.isStarted shouldBe true
                teamLogsAppender.keepAliveDuration.milliseconds shouldBe 30_000L
                teamLogsAppender.writeTimeout.milliseconds shouldBe 60_000L
                teamLogsAppender.reconnectionDelay.milliseconds shouldBe 1_000L

                val filterProbe = object : LogstashTcpSocketAppender() {
                    fun beslutning(event: ILoggingEvent): FilterReply = getFilterChainDecision(event)
                }
                teamLogsAppender.getCopyOfAttachedFiltersList().forEach(filterProbe::addFilter)
                filterProbe.beslutning(loggingEvent(teamLogger, "med marker", teamLogsMarker)) shouldBe FilterReply.ACCEPT
                filterProbe.beslutning(loggingEvent(teamLogger, "uten marker", null)) shouldBe FilterReply.DENY

                rootLogger.info("Skal maskere: 12845678912")
                rootLogger.info("Skal ikke maskere: 1234567890123")
                rootLogger.info("Skal maskere i tekst: e12845678901e")
                rootLogger.info("Skal ikke maskere: helt ufarlig tekst")
                rootLogger.info("Skal ikke maskere: 123456 12345")
                rootLogger.info("Skal ikke maskere: 123456  12345")
                val forventedeVanligeMeldinger = listOf(
                    "Skal maskere: ***********",
                    "Skal ikke maskere: 1234567890123",
                    "Skal maskere i tekst: e***********e",
                    "Skal ikke maskere: helt ufarlig tekst",
                    "Skal ikke maskere: 123456 12345",
                    "Skal ikke maskere: 123456  12345",
                )
                meldingerFraConsole(consoleOutput) shouldContainExactly forventedeVanligeMeldinger

                val sikkerloggMelding = "Sikkerlogg med fnr 12845678912"
                teamLogger.info(teamLogsMarker, sikkerloggMelding)

                serverSocket.accept().use { socket ->
                    socket.soTimeout = 5_000
                    val linje = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8).readLine()
                        ?: error("Fant ingen team-logs linje på socket.")
                    jsonMelding(linje) shouldBe sikkerloggMelding
                    linje.contains("12845678912") shouldBe true
                    meldingerFraConsole(consoleOutput) shouldContainExactly forventedeVanligeMeldinger
                    context.stop()
                }
            } finally {
                context.stop()
            }
        }
    }

    private fun isolertKonfigurasjonFraProduksjonsfilen(
        port: Int,
        lokalAdresse: String,
    ): String {
        val logbackXml = this::class.java.classLoader.getResource("logback.xml")?.readText()
            ?: error("Fant ikke logback.xml på classpath.")
        val stdoutAppender = finnPåkrevdDel(stdoutAppenderRegex, logbackXml, "STDOUT_JSON-appender")
        val teamAppender = finnPåkrevdDel(teamAppenderRegex, logbackXml, "team-logs-appender")
        val antallDestinations = destinationRegex.findAll(teamAppender).count()
        require(antallDestinations == 1) {
            "Forventet nøyaktig ett <destination>-element i team-logs-appender, fant $antallDestinations."
        }
        val lokalTeamAppender =
            teamAppender.replace(destinationRegex, "<destination>$lokalAdresse:$port</destination>")
        val teamLogger = finnPåkrevdDel(teamLoggerRegex, logbackXml, "team-logs-logger")
        val rootLogger = finnPåkrevdDel(rootLoggerRegex, logbackXml, "root-logger")

        // Testen laster bare produksjonsdelene vi verifiserer, for å unngå eksterne appendere.
        return """
            <configuration>
                $stdoutAppender
                $lokalTeamAppender
                $teamLogger
                $rootLogger
            </configuration>
        """.trimIndent()
    }

    private fun loggingEvent(logger: Logger, message: String, marker: Marker?): LoggingEvent =
        LoggingEvent(javaClass.name, logger, Level.INFO, message, null, null).apply {
            marker?.let(::addMarker)
        }

    private fun meldingerFraConsole(output: ByteArrayOutputStream): List<String> =
        output.toString(StandardCharsets.UTF_8)
            .lineSequence()
            .filter { it.isNotBlank() }
            .map(::jsonMelding)
            .toList()

    private fun jsonMelding(jsonLinje: String): String =
        objectMapper.readTree(jsonLinje).path("message").asText()

    private fun finnPåkrevdDel(regex: Regex, innhold: String, navn: String): String =
        regex.find(innhold)?.value ?: error("Fant ikke $navn i logback.xml.")

    private companion object {
        private const val LOKAL_LOOPBACK_ADRESSE = "127.0.0.1"
        private val teamLogsMarker: Marker = MarkerFactory.getMarker("TEAM_LOGS")
        private val stdoutAppenderRegex = Regex(
            """<appender\s+name="STDOUT_JSON".*?</appender>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val teamAppenderRegex = Regex(
            """<appender\s+name="team-logs".*?</appender>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val teamLoggerRegex = Regex(
            """<logger\s+name="team-logs-logger".*?</logger>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val rootLoggerRegex = Regex(
            """<root\s+level="[^"]+".*?</root>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val destinationRegex = Regex("""<destination>\s*[^<]+\s*</destination>""")
    }
}
