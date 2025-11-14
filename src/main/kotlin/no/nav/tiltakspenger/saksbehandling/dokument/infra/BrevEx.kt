package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode

/**
 * https://sprakradet.no/godt-og-korrekt-sprak/rettskriving-og-grammatikk/tall-tid-dato/
 */
fun Int.toTekst(): String = when (this) {
    1 -> "ett"
    2 -> "to"
    3 -> "tre"
    4 -> "fire"
    5 -> "fem"
    6 -> "seks"
    7 -> "syv"
    8 -> "åtte"
    9 -> "ni"
    10 -> "ti"
    11 -> "elleve"
    12 -> "tolv"
    13 -> "tretten"
    14 -> "fjorten"
    15 -> "femten"
    else -> this.toString()
}

fun AntallBarn.toTekst(): String = this.value.toTekst()

fun toAntallDagerTekst(
    antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
): String? {
    if (antallDagerPerMeldeperiode?.size != 1) {
        return null
    } else {
        val antallDager = antallDagerPerMeldeperiode.first().verdi.value
        if (antallDager == 0 || antallDager > 10 || erOddetall(antallDager)) {
            return null
        } else if (antallDager == 2) {
            return "en dag per uke"
        } else {
            val antallDagerPerUke = antallDager / 2
            return "${antallDagerPerUke.toTekst()} dager per uke"
        }
    }
}

private fun erOddetall(tall: Int): Boolean =
    tall % 2 != 0
