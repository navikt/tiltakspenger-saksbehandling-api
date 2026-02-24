package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.assertions.json.CompareJsonOptions
import io.kotest.assertions.json.equalJson
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.should

infix fun String.harKode(forventetKode: String) {
    this.shouldContainJsonKeyValue("kode", forventetKode)
}

// Tilsvarer Kotest sin shouldEqualJson, med ignorerer verdier der både actual og expected er en datetime string
fun String.shouldEqualJsonIgnoringTimestamps(expected: String): String {
    return this.replaceTimestamps().shouldEqualJson(expected.replaceTimestamps())
}

// Tilsvarer Kotest sin shouldEqualJson, med ignorerer verdier der både actual og expected er en datetime string
fun String.shouldEqualJsonIgnoringTimestamps(configureAndProvideExpected: CompareJsonOptions.() -> String): String {
    val options = CompareJsonOptions()
    val expected = options.configureAndProvideExpected().replaceTimestamps()
    this.replaceTimestamps() should equalJson(expected, options)
    return this
}

private val timestampRegex = """"(\w+)":\s*"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?"""".toRegex()

private fun String.replaceTimestamps(): String {
    return this.replace(timestampRegex) {
        """"${it.groupValues[1]}": "TIMESTAMP""""
    }
}
