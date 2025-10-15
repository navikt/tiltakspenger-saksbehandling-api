package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import com.fasterxml.jackson.databind.node.TextNode
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling

fun SendtUtbetaling.toJson(): String {
    return serialiserRequestResponse(
        request = this.request,
        response = this.response,
        responseStatus = this.responseStatus,
    )
}

fun KunneIkkeUtbetale.toJson(): String {
    return serialiserRequestResponse(
        request = this.request,
        response = this.response,
        responseStatus = this.responseStatus,
    )
}

private fun serialiserRequestResponse(
    request: String?,
    response: String?,
    responseStatus: Int?,
): String {
    return """
        {
        "request": ${request?.toValidJson()},
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
    return if (isValidJson) this else TextNode(this).toString()
}
