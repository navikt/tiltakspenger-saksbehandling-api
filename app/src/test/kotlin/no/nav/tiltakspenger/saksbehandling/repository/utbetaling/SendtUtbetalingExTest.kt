package no.nav.tiltakspenger.saksbehandling.repository.utbetaling

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SendtUtbetaling
import org.junit.jupiter.api.Test

internal class SendtUtbetalingExTest {

    @Test
    fun `empty and plain string`() {
        val actual = SendtUtbetaling("", "response", 202).toJson()
        objectMapper.readTree(actual)
        actual.shouldEqualJson(
            """
            {
            "request": "",
            "response": "response",
            "responseStatus": 202
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `blank string`() {
        val actual = SendtUtbetaling(" ", " a ", 202).toJson()
        objectMapper.readTree(actual)
        actual.shouldEqualJson(
            """
            {
            "request": "",
            "response": " a ",
            "responseStatus": 202
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `escapes non-json string`() {
        val nonEscapedString = """"\/\b\f\n\r\t"""

        val actual = SendtUtbetaling("request", nonEscapedString, 202).toJson()
        objectMapper.readTree(actual)
        actual.shouldEqualJson(
            """
            {
            "request": "request",
            "response": "\"\\\/\\b\\f\\n\\r\\t",
            "responseStatus": 202
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `valid json string`() {
        val validJsonString = """{"key": "value"}"""

        val actual = SendtUtbetaling("request", validJsonString, 202).toJson()
        objectMapper.readTree(actual)
        actual.shouldEqualJson(
            """
            {
            "request": "request",
            "response": {"key": "value"},
            "responseStatus": 202
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `valid json string with escaped characters`() {
        val validJsonString = """{"key": "value", "escaped": "\"\\\/\\b\\f\\n\\r\\t"}"""

        val actual = SendtUtbetaling("request", validJsonString, 202).toJson()
        objectMapper.readTree(actual)
        actual.shouldEqualJson(
            """
            {
            "request": "request",
            "response": {"key": "value", "escaped": "\"\\\/\\b\\f\\n\\r\\t"},
            "responseStatus": 202
            }
            """.trimIndent(),
        )
    }
}
