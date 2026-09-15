package no.nav.tiltakspenger.saksbehandling.infra.route

import tools.jackson.databind.JsonNode

/**
 * Json-en for et sladdbart felt i forventet json i rutetestene.
 * Feltet er alltid innpakningen [SladdbarVerdi], også når verdien mangler.
 */

const val SLADDET_JSON = """{"verdi": null, "erSladdet": true}"""

fun ikkeSladdetTekst(tekst: String?): String = ikkeSladdetJson(tekst?.let { "\"$it\"" })

/**
 * [verdi] skrives som json-literal, så strenger må siteres av kalleren eller sendes gjennom [ikkeSladdetTekst].
 */
fun ikkeSladdetJson(verdi: Any?): String = """{"verdi": ${verdi ?: "null"}, "erSladdet": false}"""

/**
 * Leser verdien i et sladdbart felt i json-en.
 */
fun JsonNode.sladdbarVerdi(felt: String): JsonNode = get(felt).get("verdi")
