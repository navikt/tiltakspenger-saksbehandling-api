package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.dokument.infra.BrevRammevedtakInnvilgelseBaseDTO.SatserDTO

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
    antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode>,
): String? {
    val verdier = antallDagerPerMeldeperiode.verdier.distinctBy { it.value }

    // Dersom det er ulikt antall dager i periodiseringen må saksbehandler spesifisere dette i fritekst
    if (verdier.size > 1) {
        return null
    }

    val antallDager = verdier.single().value
    if (antallDager == 0 || antallDager > 10 || erOddetall(antallDager)) {
        return null
    } else if (antallDager == 2) {
        return "en dag per uke"
    } else {
        val antallDagerPerUke = antallDager / 2
        return "${antallDagerPerUke.toTekst()} dager per uke"
    }
}

private fun erOddetall(tall: Int): Boolean =
    tall % 2 != 0

fun Satser.Companion.tilSatserDTO(periode: Periode): List<SatserDTO> {
    return satser.filter { it.periode.overlapperMed(periode) }.map {
        SatserDTO(
            år = it.periode.fraOgMed.year,
            ordinær = it.sats,
            barnetillegg = it.satsBarnetillegg,
        )
    }
}

// Joiner til komma-separert string med 'og' som siste separator
fun List<String>.joinMedKonjunksjon(): String {
    return when (this.size) {
        0 -> throw IllegalStateException("Må ha minst en string")
        1 -> this.first()
        2 -> "${this.first()} og ${this.last()}"
        else -> this.dropLast(1).joinToString(", ").plus(" og ${this.last()}")
    }
}
