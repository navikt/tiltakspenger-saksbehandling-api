package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn

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
