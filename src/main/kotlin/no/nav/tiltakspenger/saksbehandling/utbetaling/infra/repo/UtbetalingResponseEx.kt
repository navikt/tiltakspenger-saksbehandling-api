package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingResponse
import tools.jackson.databind.node.StringNode

fun UtbetalingResponse.toJson(): String {
    return """
        {
        "request": ${request.toValidJson()},
        "response": ${response?.toValidJson()},
        "responseStatus": $responseStatus
        }
    """.trimIndent()
}

private fun String.toValidJson(): String {
    if (this.isBlank()) return "\"\""
    val isValidJson = try {
        objectMapper.readTree(this)
        true
    } catch (e: Exception) {
        false
    }
    return if (isValidJson) this else StringNode(this).toString()
}
