package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson

infix fun String.harKode(forventetKode: String) {
    this.shouldContainJsonKeyValue("kode", forventetKode)
}

fun String.shouldEqualJsonIgnoringTimestamps(expectedJson: String): String {
    return this.replaceTimestamps().shouldEqualJson(expectedJson.replaceTimestamps())
}

private val timestampRegex = """"(\w+)":\s*"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?"""".toRegex()

private fun String.replaceTimestamps(): String {
    return this.replace(timestampRegex) {
        """"${it.groupValues[1]}": "TIMESTAMP""""
    }
}
