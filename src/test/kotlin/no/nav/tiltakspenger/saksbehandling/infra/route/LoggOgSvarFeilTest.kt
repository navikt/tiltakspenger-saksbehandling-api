package no.nav.tiltakspenger.saksbehandling.infra.route

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class LoggOgSvarFeilTest {

    private data class Testfeil(
        override val loggkontekst: Loggkontekst,
        override val sikkerloggkontekst: Loggkontekst? = null,
    ) : Loggbar

    private val logger = KotlinLogging.logger("LoggOgSvarFeilTest")

    private fun fangLogglinjer(loggernavn: String): ListAppender<ILoggingEvent> {
        val logbackLogger = LoggerFactory.getLogger(loggernavn) as ch.qos.logback.classic.Logger
        return ListAppender<ILoggingEvent>().apply {
            start()
            logbackLogger.addAppender(this)
        }
    }

    private fun slippLogglinjer(loggernavn: String, appender: ListAppender<ILoggingEvent>) {
        (LoggerFactory.getLogger(loggernavn) as ch.qos.logback.classic.Logger).detachAppender(appender)
    }

    private fun ApplicationTestBuilder.feilRoute(feil: Loggbar) {
        routing {
            get("/feil") {
                call.loggOgSvarFeil(
                    logger = logger,
                    operasjon = "Testoperasjon",
                    feil = feil,
                    statusOgErrorJson = HttpStatusCode.BadRequest to ErrorJson("Melding til bruker.", "test_kode"),
                    kontekst = "sakId=sak_123",
                )
            }
        }
    }

    @Test
    fun `uten sikkerloggkontekst - én warn-linje i vanlig logg og ingenting i sikkerlogg`() {
        val vanligLogg = fangLogglinjer("LoggOgSvarFeilTest")
        val sikkerlogg = fangLogglinjer("team-logs-logger")
        try {
            testApplication {
                feilRoute(Testfeil(loggkontekst = Loggkontekst("behandlingen har status TEST")))

                val response = client.get("/feil")

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldEqualJson """
                    {
                        "melding": "Melding til bruker.",
                        "kode": "test_kode"
                    }
                """
            }
            vanligLogg.list.single().also {
                it.level shouldBe Level.WARN
                it.formattedMessage shouldContain "Testoperasjon feilet: behandlingen har status TEST"
                it.formattedMessage shouldContain "'test_kode'"
                it.formattedMessage shouldContain "sakId=sak_123"
                it.formattedMessage shouldNotContain "sikkerlogg"
            }
            sikkerlogg.list.shouldBeEmpty()
        } finally {
            slippLogglinjer("LoggOgSvarFeilTest", vanligLogg)
            slippLogglinjer("team-logs-logger", sikkerlogg)
        }
    }

    @Test
    fun `med sikkerloggkontekst - vanlig logg henviser med lenke og detaljene finnes kun i sikkerlogg`() {
        val vanligLogg = fangLogglinjer("LoggOgSvarFeilTest")
        val sikkerlogg = fangLogglinjer("team-logs-logger")
        try {
            testApplication {
                feilRoute(
                    Testfeil(
                        loggkontekst = Loggkontekst("kall mot baksystem feilet"),
                        sikkerloggkontekst = Loggkontekst(
                            melding = "fnr=12345678901, rå respons: {\"detaljer\": \"sensitivt\"}",
                        ),
                    ),
                )

                client.get("/feil").status shouldBe HttpStatusCode.BadRequest
            }
            vanligLogg.list.single().also {
                it.level shouldBe Level.WARN
                it.formattedMessage shouldContain "Testoperasjon feilet: kall mot baksystem feilet"
                it.formattedMessage shouldContain "Se sikkerlogg for mer kontekst: https://console.cloud.google.com"
                it.formattedMessage shouldNotContain "12345678901"
            }
            sikkerlogg.list.single().also {
                it.level shouldBe Level.WARN
                it.formattedMessage shouldContain "Testoperasjon feilet: fnr=12345678901"
                it.formattedMessage shouldContain "sakId=sak_123"
                it.markerList.single().name shouldBe "TEAM_LOGS"
            }
        } finally {
            slippLogglinjer("LoggOgSvarFeilTest", vanligLogg)
            slippLogglinjer("team-logs-logger", sikkerlogg)
        }
    }

    @Test
    fun `med underliggende feil i sikkerloggkonteksten - stacktracen finnes kun i sikkerlogg`() {
        val vanligLogg = fangLogglinjer("LoggOgSvarFeilTest")
        val sikkerlogg = fangLogglinjer("team-logs-logger")
        try {
            testApplication {
                feilRoute(
                    Testfeil(
                        loggkontekst = Loggkontekst("kall mot baksystem feilet"),
                        sikkerloggkontekst = Loggkontekst(
                            melding = "rå respons for fnr=12345678901",
                            underliggendeFeil = RuntimeException("400 Bad Request: {\"fnr\": \"12345678901\"}"),
                        ),
                    ),
                )

                client.get("/feil").status shouldBe HttpStatusCode.BadRequest
            }
            vanligLogg.list.single().also {
                it.throwableProxy shouldBe null
                it.formattedMessage shouldContain "Se sikkerlogg for mer kontekst"
                it.formattedMessage shouldNotContain "12345678901"
            }
            sikkerlogg.list.single().also {
                it.formattedMessage shouldContain "rå respons for fnr=12345678901"
                it.throwableProxy!!.message shouldContain "{\"fnr\": \"12345678901\"}"
            }
        } finally {
            slippLogglinjer("LoggOgSvarFeilTest", vanligLogg)
            slippLogglinjer("team-logs-logger", sikkerlogg)
        }
    }
}
