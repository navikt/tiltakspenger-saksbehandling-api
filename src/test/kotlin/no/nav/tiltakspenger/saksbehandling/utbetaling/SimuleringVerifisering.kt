package no.nav.tiltakspenger.saksbehandling.utbetaling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import java.time.LocalDate

// Hjelpere for å verifisere simuleringen slik den kommer ut av routene.
// Vi asserter på JSON-en klienten faktisk får, ikke på domeneobjektene.
// Da dekker testene hele veien: responsen fra helved, mapperen, `OppsummeringGenerator` og DTO-laget.

/**
 * Plukker ut `simulertBeregning` for den eneste meldekortbehandlingen i sak-responsen.
 *
 * Feiler tydelig dersom det er flere behandlinger, slik at testen må si hvilken den mener.
 */
fun JSONObject.simulertBeregningForEnesteMeldekortbehandling(): JSONObject {
    val behandlinger = getJSONObject("meldekortbehandlinger")
    behandlinger.length() shouldBe 1
    val behandling = behandlinger.getJSONObject(behandlinger.keys().next().toString())
    require(!behandling.isNull("simulertBeregning")) {
        "Meldekortbehandlingen mangler simulertBeregning. Nøkler: ${behandling.keys().asSequence().toList()}"
    }
    return behandling.getJSONObject("simulertBeregning")
}

/** Verifiserer de oppsummerte beløpene for hele behandlingen. */
fun JSONObject.verifiserSimulerteBeløp(@Language("JSON") forventet: String) {
    getJSONObject("simulerteBeløp").toString().shouldEqualJsonIgnoringTimestamps(forventet)
}

/** Verifiserer resultat, dato og totalbeløp for simuleringen. */
fun JSONObject.verifiserSimuleringsoppsummering(
    forventetResultat: String,
    forventetTotalBeløp: Int,
    forventetSimuleringsdato: LocalDate,
) {
    getString("simuleringResultat") shouldBe forventetResultat
    getInt("simuleringTotalBeløp") shouldBe forventetTotalBeløp
    getString("simuleringsdato") shouldBe forventetSimuleringsdato.toString()
}

/**
 * Verifiserer posteringene for én dag.
 *
 * Posteringene er det vi fikk fra helved, uendret gjennom hele kjeden.
 * Det er her en klassekode eller en posteringstype som ikke overlever mappingen ville dukket opp.
 */
fun JSONObject.verifiserPosteringerForDag(dato: LocalDate, @Language("JSON") forventet: String) {
    hentDag(dato).getJSONArray("posteringer").toString().shouldEqualJsonIgnoringTimestamps(forventet)
}

/** Verifiserer de simulerte beløpene for én dag. */
fun JSONObject.verifiserSimulerteBeløpForDag(dato: LocalDate, @Language("JSON") forventet: String) {
    hentDag(dato).getJSONObject("simulerteBeløp").toString().shouldEqualJsonIgnoringTimestamps(forventet)
}

private fun JSONObject.hentDag(dato: LocalDate): JSONObject {
    val meldeperioder = getJSONArray("meldeperioder")
    val dager = (0 until meldeperioder.length())
        .map { meldeperioder.getJSONObject(it) }
        .flatMap { meldeperiode ->
            val dagerJson = meldeperiode.getJSONArray("dager")
            (0 until dagerJson.length()).map { dagerJson.getJSONObject(it) }
        }
    return dager.singleOrNull { it.getString("dato") == dato.toString() }
        ?: throw AssertionError("Fant ikke dagen $dato. Dager: ${dager.map { it.getString("dato") }}")
}
