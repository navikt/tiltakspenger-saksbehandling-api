package no.nav.tiltakspenger.vedtak.repository.utbetaling

import com.fasterxml.jackson.databind.node.TextNode
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.ports.SendtUtbetaling

fun SendtUtbetaling.toJson(): String {
    return """
        {
        "request": ${this.request.toValidJson()},
        "response": ${this.response.toValidJson()},
        "responseStatus": ${this.responseStatus}
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
    return if (isValidJson) this else TextNode(this).toString()
}
