package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.matchers.shouldBe
import tools.jackson.databind.JsonNode

fun JsonNode.koder(felt: String): List<String> = get(felt).toList().map { it.asString() }

/**
 * pdfgenrs lager hjemmelteksten selv når tekstfeltene mangler.
 * Feltene må mangle, ikke være `null`, fordi pdfgenrs tolker `valgtHjemmelTekst: null` i opphørsbrev som «ingen standardtekst».
 */
fun JsonNode.skalIkkeHaHjemmeltekst() {
    has("valgtHjemmelTekst") shouldBe false
    has("hjemlerTekst") shouldBe false
}
